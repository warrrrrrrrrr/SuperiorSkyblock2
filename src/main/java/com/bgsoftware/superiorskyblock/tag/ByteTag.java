/*
 * JNBT License

Copyright (c) 2010 Graham Edgecombe
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
      
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
      
    * Neither the name of the JNBT team nor the names of its
      contributors may be used to endorse or promote products derived from
      this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE. 
 */
package com.bgsoftware.superiorskyblock.tag;

import com.bgsoftware.superiorskyblock.utils.debug.PluginDebugger;
import com.google.common.base.Preconditions;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * The <code>TAG_Byte</code> tag.
 *
 * @author Graham Edgecombe
 */
public final class ByteTag extends Tag<Byte> {

    static final Class<?> CLASS = getNNTClass("NBTTagByte");

    /**
     * Creates the tag.
     *
     * @param value The value.
     */
    public ByteTag(byte value) {
        super(value, CLASS, byte.class);
    }

    public static ByteTag fromNBT(Object tag) {
        Preconditions.checkArgument(tag.getClass().equals(CLASS), "Cannot convert " + tag.getClass() + " to ByteTag!");

        try {
            byte value = plugin.getNMSTags().getNBTByteValue(tag);
            return new ByteTag(value);
        } catch (Exception ex) {
            ex.printStackTrace();
            PluginDebugger.debug(ex);
            return null;
        }
    }

    public static ByteTag fromStream(DataInputStream is) throws IOException {
        return new ByteTag(is.readByte());
    }

    @Override
    protected void writeData(DataOutputStream os) throws IOException {
        os.writeByte(value);
    }

}