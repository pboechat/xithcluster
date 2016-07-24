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
package org.xith3d.utility.sgtree.infoitems;

import org.openmali.vecmath2.Colorf;

import org.xith3d.scenegraph.Texture;

/**
 * Xith3DTree
 * 
 * Display a Xith3D scenegraph in a Swing Tree control
 * 
 * @author Daniel Selman (Java3D version)
 * @author Hawkwind
 * @author Amos Wenger (aka BlueSky)
 */
public class SGTInfo_Texture extends SGTInfo_Object
{
    /*
     * private static final int[] m_kCapabilityArray = {
     * Texture.ALLOW_BOUNDARY_COLOR_READ, Texture.ALLOW_BOUNDARY_MODE_READ,
     * Texture.ALLOW_ENABLE_READ, Texture.ALLOW_FILTER_READ,
     * Texture.ALLOW_IMAGE_READ, Texture.ALLOW_MIPMAP_MODE_READ };
     */

    public SGTInfo_Texture()
    {
    }
    
    @Override
    java.util.List< ? > getChildren( Object obj )
    {
        return super.getChildren( obj );
    }
    
    @Override
    public int[] getCapabilityBits()
    {
        // return createCompoundArray( m_kCapabilityArray,
        // super.getCapabilityBits() );
        return super.getCapabilityBits();
    }
    
    @Override
    public String getInfo( Object obj )
    {
        String szText = super.getInfo( obj );
        
        szText = insertSectionBreak( szText );
        szText += "Texture\r\n";
        
        Texture texture = (Texture)obj;
        
        Colorf color = new Colorf();
        texture.getBoundaryColor( color );
        
        szText += "Boundary Color: " + color + "\r\n";
        szText += "Boundary Mode S: " + texture.getBoundaryModeS() + "\r\n";
        szText += "Boundary Mode T: " + texture.getBoundaryModeT() + "\r\n";
        szText += "Filter: " + texture.getFilter() + "\r\n";
        szText += "Enable: " + texture.isEnabled() + "\r\n";
        szText += "MipMap Mode: " + texture.getMipMapMode() + "\r\n";
        
        return szText;
    }
}
