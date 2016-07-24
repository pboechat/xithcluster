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
package org.xith3d.terrain;

import java.net.URL;

/**
 * 
 * @author Mathias 'cylab' Henze
 */
public class L3DTerrain extends ChunkedTerrain
{
    private static class L3DTSpec extends Spec
    {
        public L3DTSpec( URL location, float unitSize )
        {
            L3DTResourceProvider provider = new L3DTResourceProvider( location );
            float width = provider.getNx() * provider.getHScale() * unitSize;
            float depth = provider.getNy() * provider.getHScale() * unitSize;
            float height = ( provider.getMaxAlt() - provider.getMinAlt() ) * unitSize;
            this.resourceProvider = provider;
            this.x = -( width / 2 );
            this.y = provider.getMinAlt() * unitSize;
            this.z = -( depth / 2 );
            this.scale = width;
            this.height = height;
        }
    }
    
    public L3DTerrain( URL location )
    {
        super( new L3DTSpec( location, 1 ) );
    }
    
    public L3DTerrain( URL location, float unitSize )
    {
        super( new L3DTSpec( location, unitSize ) );
    }
}
