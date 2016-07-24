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
package org.xith3d.utility.properties;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * :Id: FilePropertyWriter.java,v 1.5 2003/02/24 00:13:53 wurp Exp $
 * 
 * :Log: FilePropertyWriter.java,v $
 * Revision 1.5  2003/02/24 00:13:53  wurp
 * Formatted all java code for cvs (strictSunConvention.xml)
 * 
 * Revision 1.4  2001/06/20 04:05:42  wurp
 * added log4j.
 * 
 * Revision 1.3  2001/01/28 07:52:20  wurp
 * Removed <dollar> from Id and Log in log comments.
 * Added several new commands to AdminApp
 * Unfortunately, several other changes that I have lost track of.  Try diffing this
 * version with the previous one.
 * 
 * Revision 1.2  2000/12/16 22:07:33  wurp
 * Added Id and Log to almost all of the files that didn't have it.  It's
 * possible that the script screwed something up.  I did a commit and an update
 * right before I ran the script, so if a file is screwed up you should be able
 * to fix it by just going to the version before this one.
 * 
 * @author David Yazel
 */
public class FilePropertyWriter implements PropertyWriter
{
    private PrintStream prnout;
    private int indent;
    
    private void writeIndent()
    {
        for ( int i = 0; i < indent; i++ )
            prnout.print( " " );
    }
    
    public void write( String name, int value )
    {
        writeIndent();
        prnout.print( name + ":int=" );
        prnout.println( value );
    }
    
    public void write( String name, float value )
    {
        writeIndent();
        prnout.print( name + ":float=" );
        prnout.println( value );
    }
    
    public void write( String name, double value )
    {
        writeIndent();
        prnout.print( name + ":double=" );
        prnout.println( value );
    }
    
    public void write( String name, String value )
    {
        writeIndent();
        prnout.print( name + ":string=" );
        prnout.println( value );
    }
    
    public void write( Property value )
    {
        writeIndent();
        
        // write out the header
        prnout.print( "<" );
        prnout.print( value.getName() );
        prnout.print( ":" );
        prnout.print( ( (Object)value ).getClass().getName() );
        prnout.println( ">" );
        
        indent += 3;
        indent -= 3;
        
        prnout.print( "</" );
        prnout.print( value.getName() );
        prnout.println( ">" );
    }
    
    void close()
    {
        prnout.flush();
        prnout.close();
    }
    
    public FilePropertyWriter( String filename ) throws java.io.IOException
    {
        prnout = new PrintStream( new BufferedOutputStream( new FileOutputStream( filename ) ) );
        indent = 0;
    }
}
