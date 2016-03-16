/*
 *  Copyright 2013~2014 Dan Haywood
 *
 *  Licensed under the Apache License, Version 2.0 (the
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
package org.apache.isis.applib.services.metamodel;

public interface DomainMember extends Comparable<DomainMember> {

    String getClassType();

    String getClassName();

    String getPackageName();

    String getType();

    String getMemberName();

    String getNumParams();

    boolean isContributed();

    String getContributedBy();

    boolean isMixedIn();

    String getMixin();

    String getHidden();

    String getDisabled();

    String getChoices();

    String getAutoComplete();

    String getDefault();

    String getValidate();
}