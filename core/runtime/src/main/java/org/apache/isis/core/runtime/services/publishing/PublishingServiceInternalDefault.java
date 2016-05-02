/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.isis.core.runtime.services.publishing;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.apache.isis.applib.Identifier;
import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.annotation.NatureOfService;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.annotation.PublishedAction;
import org.apache.isis.applib.annotation.PublishedObject;
import org.apache.isis.applib.annotation.PublishedObject.ChangeKind;
import org.apache.isis.applib.services.bookmark.Bookmark;
import org.apache.isis.applib.services.bookmark.BookmarkService;
import org.apache.isis.applib.services.clock.ClockService;
import org.apache.isis.applib.services.command.Command;
import org.apache.isis.applib.services.command.CommandContext;
import org.apache.isis.applib.services.iactn.Interaction;
import org.apache.isis.applib.services.iactn.InteractionContext;
import org.apache.isis.applib.services.publish.EventMetadata;
import org.apache.isis.applib.services.publish.EventPayload;
import org.apache.isis.applib.services.publish.EventType;
import org.apache.isis.applib.services.publish.ObjectStringifier;
import org.apache.isis.applib.services.publish.PublisherService;
import org.apache.isis.applib.services.publish.PublishingService;
import org.apache.isis.applib.services.user.UserService;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.adapter.oid.Oid;
import org.apache.isis.core.metamodel.adapter.oid.OidMarshaller;
import org.apache.isis.core.metamodel.adapter.oid.RootOid;
import org.apache.isis.core.metamodel.facetapi.IdentifiedHolder;
import org.apache.isis.core.metamodel.facets.FacetedMethod;
import org.apache.isis.core.metamodel.facets.FacetedMethodParameter;
import org.apache.isis.core.metamodel.facets.actions.action.invocation.CommandUtil;
import org.apache.isis.core.metamodel.facets.actions.publish.PublishedActionFacet;
import org.apache.isis.core.metamodel.facets.object.encodeable.EncodableFacet;
import org.apache.isis.core.metamodel.facets.object.publishedobject.PublishedObjectFacet;
import org.apache.isis.core.metamodel.services.command.CommandMementoService;
import org.apache.isis.core.metamodel.services.publishing.PublishingServiceInternal;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.metamodel.spec.feature.ObjectAction;
import org.apache.isis.core.runtime.services.enlist.EnlistedObjectsServiceInternal;
import org.apache.isis.core.runtime.system.context.IsisContext;
import org.apache.isis.core.runtime.system.persistence.PersistenceSession;
import org.apache.isis.core.runtime.system.transaction.IsisTransactionManager;
import org.apache.isis.schema.aim.v2.ActionInvocationMementoDto;
import org.apache.isis.schema.aim.v2.ReturnDto;
import org.apache.isis.schema.cmd.v1.ActionDto;
import org.apache.isis.schema.utils.ActionInvocationMementoDtoUtils;

/**
 * Wrapper around {@link PublishingService}.  Is a no-op if there is no injected service.
 */
@DomainService(nature = NatureOfService.DOMAIN)
public class PublishingServiceInternalDefault implements PublishingServiceInternal {

    private final static Function<ObjectAdapter, ObjectAdapter> NOT_DESTROYED_ELSE_EMPTY = new Function<ObjectAdapter, ObjectAdapter>() {
        public ObjectAdapter apply(ObjectAdapter adapter) {
            if(adapter == null) {
                return null;
            }
            if (!adapter.isDestroyed()) {
                return adapter;
            }
            // objectstores such as JDO prevent the underlying pojo from being touched once it has been deleted.
            // we therefore replace that pojo with an 'empty' one.

            Object replacementObject = getPersistenceSession().instantiateAndInjectServices(adapter.getSpecification());
            getPersistenceSession().remapRecreatedPojo(adapter, replacementObject);
            return adapter;
        }
        protected PersistenceSession getPersistenceSession() {
            return IsisContext.getPersistenceSession();
        }

    };

    @Programmatic
    public static EventType eventTypeFor(ChangeKind changeKind) {
        if(changeKind == ChangeKind.UPDATE) {
            return EventType.OBJECT_UPDATED;
        }
        if(changeKind == ChangeKind.CREATE) {
            return EventType.OBJECT_CREATED;
        }
        if(changeKind == ChangeKind.DELETE) {
            return EventType.OBJECT_DELETED;
        }
        throw new IllegalArgumentException("unknown ChangeKind '" + changeKind + "'");
    }

    @Override
    @Programmatic
    public void publishObjects() {

        if(publishingServiceIfAny == null) {
            return;
        }

        final Map<ObjectAdapter, ChangeKind> changeKindByEnlistedAdapter =
                enlistedObjectsServiceInternal.getChangeKindByEnlistedAdapter();

        final String currentUser = userService.getUser().getName();
        final Timestamp timestamp = clockService.nowAsJavaSqlTimestamp();
        final ObjectStringifier stringifier = objectStringifier();

        // take a copy of enlisted adapters ... the JDO implementation of the PublishingService
        // creates further entities which would be enlisted; taking copy of the keys avoids ConcurrentModificationException
        final List<ObjectAdapter> enlistedAdapters =
                Lists.newArrayList(changeKindByEnlistedAdapter.keySet());

        for (final ObjectAdapter enlistedAdapter : enlistedAdapters) {
            final ChangeKind changeKind = changeKindByEnlistedAdapter.get(enlistedAdapter);

            publishObject(enlistedAdapter, changeKind, currentUser, timestamp, stringifier);
        }
    }

    private void publishObject(
            final ObjectAdapter enlistedAdapter,
            final ChangeKind changeKind,
            final String currentUser,
            final Timestamp timestamp,
            final ObjectStringifier stringifier) {

        final PublishedObjectFacet publishedObjectFacet =
                enlistedAdapter.getSpecification().getFacet(PublishedObjectFacet.class);
        if(publishedObjectFacet == null) {
            return;
        }
        final PublishedObject.PayloadFactory payloadFactory = publishedObjectFacet.value();

        final RootOid enlistedAdapterOid = (RootOid) enlistedAdapter.getOid();
        final String enlistedAdapterClass = CommandUtil.targetClassNameFor(enlistedAdapter);
        final Bookmark enlistedTarget = enlistedAdapterOid.asBookmark();

        final EventMetadata metadata = newEventMetadata(
                currentUser, timestamp, changeKind, enlistedAdapterClass, enlistedTarget);

        final Object pojo = ObjectAdapter.Util.unwrap(undeletedElseEmpty(enlistedAdapter));
        final EventPayload payload = payloadFactory.payloadFor(pojo, changeKind);

        payload.withStringifier(stringifier);
        publishingServiceIfAny.publish(metadata, payload);
    }

    @Programmatic
    public void publishAction(
            final Interaction.Execution execution,
            final ObjectAction objectAction,
            final IdentifiedHolder identifiedHolder,
            final ObjectAdapter targetAdapter,
            final List<ObjectAdapter> parameterAdapters,
            final ObjectAdapter resultAdapter) {

        publishActionToPublishingService(
                objectAction, identifiedHolder, targetAdapter, parameterAdapters, resultAdapter
        );

        publishActionToPublisherServices(
                objectAction, identifiedHolder, targetAdapter, parameterAdapters, resultAdapter,
                execution);

    }

    private void publishActionToPublishingService(
            final ObjectAction objectAction,
            final IdentifiedHolder identifiedHolder,
            final ObjectAdapter targetAdapter,
            final List<ObjectAdapter> parameterAdapters,
            final ObjectAdapter resultAdapter) {
        if(publishingServiceIfAny == null) {
            return;
        }
        final String currentUser = userService.getUser().getName();
        final Timestamp timestamp = clockService.nowAsJavaSqlTimestamp();

        final PublishedActionFacet publishedActionFacet =
                identifiedHolder.getFacet(PublishedActionFacet.class);
        if(publishedActionFacet == null) {
            return;
        }

        final RootOid adapterOid = (RootOid) targetAdapter.getOid();
        final String oidStr = getOidMarshaller().marshal(adapterOid);
        final Identifier actionIdentifier = objectAction.getIdentifier();
        final String title = oidStr + ": " + actionIdentifier.toNameParmsIdentityString();

        final String actionTargetClass = CommandUtil.targetClassNameFor(targetAdapter);
        final String actionTargetAction = CommandUtil.targetActionNameFor(objectAction);
        final Bookmark actionTarget = CommandUtil.bookmarkFor(targetAdapter);
        final String actionMemberIdentifier = CommandUtil.actionIdentifierFor(objectAction);

        final List<String> parameterNames;
        final List<Class<?>> parameterTypes;
        final Class<?> returnType;

        if(identifiedHolder instanceof FacetedMethod) {
            // should always be the case

            final FacetedMethod facetedMethod = (FacetedMethod) identifiedHolder;
            returnType = facetedMethod.getType();

            final List<FacetedMethodParameter> parameters = facetedMethod.getParameters();
            parameterNames = immutableList(Iterables.transform(parameters, FacetedMethodParameter.Functions.GET_NAME));
            parameterTypes = immutableList(Iterables.transform(parameters, FacetedMethodParameter.Functions.GET_TYPE));
        } else {
            parameterNames = null;
            parameterTypes = null;
            returnType = null;
        }

        final Command command = commandContext.getCommand();

        final Interaction.SequenceName sequenceName = Interaction.SequenceName.PUBLISHED_EVENT;
        final int nextEventSequence = command.next(sequenceName.abbr());
        final UUID transactionId = command.getTransactionId();
        final EventMetadata metadata = new EventMetadata(
                transactionId, nextEventSequence, EventType.ACTION_INVOCATION, currentUser, timestamp, title,
                actionTargetClass, actionTargetAction, actionTarget, actionMemberIdentifier, parameterNames,
                parameterTypes, returnType);

        final PublishedAction.PayloadFactory payloadFactory = publishedActionFacet.value();

        final ObjectStringifier stringifier = objectStringifier();

        final EventPayload payload = payloadFactory.payloadFor(
                identifiedHolder.getIdentifier(),
                ObjectAdapter.Util.unwrap(undeletedElseEmpty(targetAdapter)),
                ObjectAdapter.Util.unwrap(undeletedElseEmpty(parameterAdapters)),
                ObjectAdapter.Util.unwrap(undeletedElseEmpty(resultAdapter)));
        payload.withStringifier(stringifier);
        publishingServiceIfAny.publish(metadata, payload);
    }

    private static <T> List<T> immutableList(final Iterable<T> iterable) {
        return Collections.unmodifiableList(Lists.newArrayList(iterable));
    }

    private ObjectStringifier objectStringifier() {
        return new ObjectStringifier() {
                @Override
                public String toString(Object object) {
                    if(object == null) {
                        return null;
                    }
                    final ObjectAdapter adapter = IsisContext.getPersistenceSession().adapterFor(object);
                    Oid oid = adapter.getOid();
                    return oid != null? oid.enString(getOidMarshaller()): encodedValueOf(adapter);
                }
                private String encodedValueOf(ObjectAdapter adapter) {
                    EncodableFacet facet = adapter.getSpecification().getFacet(EncodableFacet.class);
                    return facet != null? facet.toEncodedString(adapter): adapter.toString();
                }
                @Override
                public String classNameOf(Object object) {
                    final ObjectAdapter adapter = getPersistenceSession().adapterFor(object);
                    final String className = adapter.getSpecification().getFullIdentifier();
                    return className;
                }
            };
    }

    private static List<ObjectAdapter> undeletedElseEmpty(List<ObjectAdapter> parameters) {
        return Lists.newArrayList(Iterables.transform(parameters, NOT_DESTROYED_ELSE_EMPTY));
    }

    private static ObjectAdapter undeletedElseEmpty(ObjectAdapter adapter) {
        return NOT_DESTROYED_ELSE_EMPTY.apply(adapter);
    }

    protected OidMarshaller getOidMarshaller() {
        return IsisContext.getOidMarshaller();
    }

    private EventMetadata newEventMetadata(
            final String currentUser,
            final Timestamp timestamp,
            final ChangeKind changeKind,
            final String enlistedAdapterClass,
            final Bookmark enlistedTarget) {
        final EventType eventType = PublishingServiceInternalDefault.eventTypeFor(changeKind);

        final Command command = commandContext.getCommand();

        final Interaction.SequenceName sequenceName = Interaction.SequenceName.PUBLISHED_EVENT;
        final int nextEventSequence = command.next(sequenceName.abbr());
        final UUID transactionId = command.getTransactionId();
        return new EventMetadata(
                transactionId, nextEventSequence, eventType, currentUser, timestamp, enlistedTarget.toString(),
                enlistedAdapterClass, null, enlistedTarget, null, null, null, null);
    }

    private void publishActionToPublisherServices(
            final ObjectAction objectAction,
            final IdentifiedHolder identifiedHolder,
            final ObjectAdapter targetAdapter,
            final List<ObjectAdapter> parameterAdapters,
            final ObjectAdapter resultAdapter,
            final Interaction.Execution execution) {

        if(publisherServices == null || publisherServices.isEmpty()) {
            return;
        }

        final Command command = commandContext.getCommand();

        final Interaction.SequenceName sequenceName = Interaction.SequenceName.PUBLISHED_EVENT;
        final int nextEventSequence = command.next(sequenceName.abbr());
        final UUID transactionId = command.getTransactionId();

        final Object targetPojo = execution.getMemberArgs().getTarget();
        final Bookmark targetBookmark = bookmarkService.bookmarkFor(targetPojo);

        final String actionClassNameId = execution.getMemberArgs().getMemberId();
        final String actionId = actionClassNameId.substring(actionClassNameId.indexOf('#')+1);

        final String title = targetBookmark.toString() + ": " + actionId;

        final String currentUser = userService.getUser().getName();
        final Timestamp startedAt = execution.getStartedAt();
        final Timestamp completedAt = execution.getCompletedAt();

        final ActionDto actionDto = new ActionDto();
        commandMementoService.addActionArgs(
                objectAction, actionDto, parameterAdapters.toArray(new ObjectAdapter[]{}));

        final ObjectSpecification returnSpec = objectAction.getReturnType();


        final Class<?> returnType = returnSpec.getCorrespondingClass();
        final ObjectAdapter argAdapter = resultAdapter;
        final Object resultPojo = argAdapter != null? argAdapter.getObject(): null;

        final ReturnDto returnDto = new ReturnDto();
        ActionInvocationMementoDtoUtils.setValue(returnDto, returnType, resultPojo);

        for (PublisherService publisherService : publisherServices) {
            final ActionInvocationMementoDto aimDto =
                ActionInvocationMementoDtoUtils.newDto(
                        transactionId,
                        nextEventSequence,
                        targetBookmark,
                        actionDto, title,
                        currentUser,
                        startedAt, completedAt,
                        returnDto);

            ActionInvocationMementoDtoUtils.addReturn(aimDto, returnType, resultPojo, bookmarkService);

            publisherService.publish(aimDto);
        }
    }


    private IsisTransactionManager.PersistenceSessionTransactionManagement getPersistenceSession() {
        return IsisContext.getPersistenceSession();
    }


    @Inject
    private List<PublisherService> publisherServices;

    @Inject
    private PublishingService publishingServiceIfAny;

    @Inject
    CommandMementoService commandMementoService;

    @Inject
    private BookmarkService bookmarkService;

    @Inject
    private EnlistedObjectsServiceInternal enlistedObjectsServiceInternal;

    @Inject
    private InteractionContext interactionContext;

    @Inject
    private CommandContext commandContext;

    @Inject
    private ClockService clockService;

    @Inject
    private UserService userService;


}
