package org.nakedobjects.object.defaults;


import org.nakedobjects.object.Oid;
import org.nakedobjects.object.io.TransferableReader;
import org.nakedobjects.object.io.TransferableWriter;


public class SerialOid implements Oid {
    private final long serialNo;
   
    public SerialOid(long serialNo) {
        this.serialNo = serialNo;
    }
    
    public SerialOid(TransferableReader reader) {
        serialNo = reader.readInt();
    }

     public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof SerialOid) {
            return ((SerialOid) obj).serialNo == serialNo;
        }
        return false;
    }

     public long getSerialNo() {
        return serialNo;
    }

    public int hashCode() {
        return 37 * 17 + (int) (serialNo ^ (serialNo >>> 32));
    }

    public String toString() {
        return "OID#" + Long.toHexString(serialNo).toUpperCase();
    }

    public void writeData(TransferableWriter writer) {
        writer.writeLong(serialNo);
    }
}

/*
Naked Objects - a framework that exposes behaviourally complete
business objects directly to the user.
Copyright (C) 2000 - 2003  Naked Objects Group Ltd

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