/**
 * Copyright (c) 2003-2010, Xith3D Project Group all rights reserved.
 * 
 * Portions based on the Java3D interface, Copyright by Sun Microsystems.
 * Many thanks to the developers of Java3D and Sun Microsystems for their
 * innovation and design.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * Neither the name of the 'Xith3D Project Group' nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) A
 * RISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE
 */
package org.xith3d.sound.drivers.lwjgl;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import org.xith3d.sound.BufferFormat;
import org.xith3d.sound.SoundBuffer;
import org.xith3d.utility.logging.X3DLog;

/**
 * LWJGL OpenAL implementation of SoundBuffer.
 * 
 * @author David Yazel
 * @author Agrv
 */
public class SoundBufferImpl implements SoundBuffer
{
    protected int handle;
    
    protected SoundBufferImpl()
    {
        IntBuffer buffer = BufferUtils.createIntBuffer(1);
        AL10.alGenBuffers( buffer );
        handle = buffer.get( 0 );
        X3DLog.debug( "A new buffer with handle ", handle, " has been allocated" );
    }
    
    public void setData( BufferFormat format, int size, int frequency, ByteBuffer data )
    {
        X3DLog.debug( "buffer is size=", size, ", freq=", frequency, ", format", format );
        
        int f = 0;
        if ( format == BufferFormat.MONO16 )
            f = AL10.AL_FORMAT_MONO16;
        else if ( format == BufferFormat.MONO8 )
            f = AL10.AL_FORMAT_MONO8;
        else if ( format == BufferFormat.STEREO16 )
            f = AL10.AL_FORMAT_STEREO16;
        else if ( format == BufferFormat.STEREO8 )
            f = AL10.AL_FORMAT_STEREO8;
        
        data.rewind();
        
        AL10.alBufferData( handle, f, data, frequency );
    }
}
