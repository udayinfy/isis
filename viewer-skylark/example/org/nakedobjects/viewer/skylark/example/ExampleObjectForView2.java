package org.nakedobjects.viewer.skylark.example;
import org.nakedobjects.object.DummyNakedObjectSpecification;
import org.nakedobjects.object.Naked;
import org.nakedobjects.object.NakedObject;
import org.nakedobjects.object.NakedObjectSpecification;
import org.nakedobjects.object.NakedValue;
import org.nakedobjects.object.Persistable;
import org.nakedobjects.object.control.Hint;
import org.nakedobjects.object.persistence.Oid;
import org.nakedobjects.object.reflect.Action;
import org.nakedobjects.object.reflect.ActionParameterSet;
import org.nakedobjects.object.reflect.MemberIdentifier;
import org.nakedobjects.object.reflect.NakedObjectAssociation;
import org.nakedobjects.object.reflect.NakedObjectField;
import org.nakedobjects.object.reflect.OneToManyAssociation;
import org.nakedobjects.object.reflect.OneToOneAssociation;
import org.nakedobjects.object.reflect.OneToOnePeer;


public class ExampleObjectForView2 implements NakedObject {

    private NakedObject object;

    public void clearCollection(OneToManyAssociation association) {}

    public void clearValue(OneToOneAssociation association) {}

    public void created() {}

    public void deleted() {}

    public NakedObject getAssociation(OneToOneAssociation field) {
        return null;
    }

    public Naked getField(NakedObjectField field) {
        return null;
    }

    public NakedObjectField[] getFields() {
        final DummyNakedObjectSpecification spec = new DummyNakedObjectSpecification();
        
        OneToOnePeer peer = new OneToOnePeer() {

            public void clearAssociation(MemberIdentifier identifier, NakedObject inObject, NakedObject associate) {}

            public Naked getAssociation(MemberIdentifier identifier, NakedObject inObject) {
                return null;
            }

            public Object getExtension(Class cls) {
                return null;
            }

            public Hint getHint(MemberIdentifier identifier, NakedObject object, Naked value) {
                return null;
            }

            public String getName() {
                return "field";
            }

            public NakedObjectSpecification getType() {
                return spec;
            }

            public boolean hasHint() {
                return false;
            }

            public void initAssociation(MemberIdentifier identifier, NakedObject inObject, NakedObject associate) {}

            public void initValue(MemberIdentifier identifier, NakedObject inObject, Object associate) {}

            public boolean isDerived() {
                return false;
            }

            public boolean isEmpty(MemberIdentifier identifier, NakedObject inObject) {
                return false;
            }

            public void setAssociation(MemberIdentifier identifier, NakedObject inObject, NakedObject associate) {}

            public void setValue(MemberIdentifier identifier, NakedObject inObject, Object associate) {}};
        
        NakedObjectField[] fields = new NakedObjectField[] {
                new OneToOneAssociation("cls", "fld", spec, peer),
/*                new OneToManyAssociation() */
        };
        return fields;
    }

    public String getIconName() {
        return null;
    }

    public String getLabel(Action action) {
        return "label";
    }

    public String getLabel(NakedObjectField field) {
        return "label";
    }

    public ActionParameterSet getParameters(Action action) {
        return null;
    }

    public NakedValue getValue(OneToOneAssociation field) {
        return null;
    }

    public NakedObjectField[] getVisibleFields() {
        return getFields();
    }

    public void initAssociation(NakedObjectAssociation field, NakedObject associatedObject) {}

    public void initOneToManyAssociation(OneToManyAssociation association, NakedObject[] instances) {}

    public void initValue(OneToOneAssociation field, Object object) {}

    public boolean isEmpty(NakedObjectField field) {
        return false;
    }

    public boolean isParsable() {
        return false;
    }

    public boolean isPersistent() {
        return false;
    }

    public boolean isResolved() {
        return false;
    }

    public Persistable persistable() {
        return null;
    }

    public void setAssociation(NakedObjectAssociation field, NakedObject associatedObject) {}

    public void setOid(Oid oid) {}

    public void setResolved() {}

    public void setValue(OneToOneAssociation field, Object object) {}

    public void clearAssociation(NakedObjectAssociation specification, NakedObject ref) {}

    public void copyObject(Naked object) {}

    public Naked execute(Action action, Naked[] parameters) {
        return null;
    }

    public Hint getHint(Action action, Naked[] parameters) {
        return null;
    }

    public Hint getHint(NakedObjectField field, Naked value) {
        return null;
    }

    public Object getObject() {
        return object;
    }

    public Oid getOid() {
        return null;
    }

    public NakedObjectSpecification getSpecification() {
        return new ExampleSpecification();
    }

    public String titleString() {
        return "Object Title";
    }
    
    public String toString() {
        return "ExampleObjectForView";
    }

    public void debugClearResolved() {}

    public void setObject(NakedObject object) {
        this.object = object;
    }

    public long getVersion() {
        return 0;
    }

    public void setVersion(long version) {}

}


/*
Naked Objects - a framework that exposes behaviourally complete
business objects directly to the user.
Copyright (C) 2000 - 2005  Naked Objects Group Ltd

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

The authors can be contacted via www.nakedobjects.org (the
registered address of Naked Objects Group is Kingsway House, 123 Goldworth
Road, Woking GU21 1NR, UK).
*/