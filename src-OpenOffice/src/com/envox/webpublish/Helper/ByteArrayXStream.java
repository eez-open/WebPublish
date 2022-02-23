/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.envox.webpublish.Helper;

import com.sun.star.io.XInputStream;
import com.sun.star.io.XOutputStream;
import com.sun.star.io.XSeekable;
import com.sun.star.io.XStream;

/**
 *
 * @author martin
 */
public class ByteArrayXStream implements XInputStream, XOutputStream, XSeekable, XStream {

    // Keep data about our byte array (we read and write to the same byte array)

    private int initialSize = 100240; // 10 kb
    private int size = 0;             // The current buffer size
    private int position = 0;         // The current write position, always<=size
    private int readPosition = 0;     // The current read position, always<=position
    private boolean closed = false;   // The XStream is closed
    private byte[] buffer;            // The buffer

    // Constructor: Initialize the byte array

    public ByteArrayXStream() {
        size = initialSize;
        buffer = new byte[size];
    }

    // Implementation of XOutputStream

    public void closeOutput()
        throws com.sun.star.io.NotConnectedException,
            com.sun.star.io.BufferSizeExceededException,
            com.sun.star.io.IOException {

        // trim buffer
        if ( buffer.length > position) {
            byte[] newBuffer = new byte[position];
            System.arraycopy(buffer, 0, newBuffer, 0, position);
            buffer = newBuffer;
        }
        closed = true;
    }

    public void flush()
        throws com.sun.star.io.NotConnectedException,
            com.sun.star.io.BufferSizeExceededException,
            com.sun.star.io.IOException {
    }

    public void writeBytes(byte[] values)
        throws com.sun.star.io.NotConnectedException,
            com.sun.star.io.BufferSizeExceededException,
            com.sun.star.io.IOException {
        if ( values.length > size-position ) {
            byte[] newBuffer = null;
            while ( values.length > size-position )
                size *= 2;
            newBuffer = new byte[size];
            System.arraycopy(buffer, 0, newBuffer, 0, position);
            buffer = newBuffer;
        }
        System.arraycopy(values, 0, buffer, position, values.length);
        position += values.length;
    }

    // Implementation of XInputStream

    private void _check() throws com.sun.star.io.NotConnectedException, com.sun.star.io.IOException {
        if(closed) {
            throw new com.sun.star.io.IOException("input closed");
        }
    }

    public int available() throws com.sun.star.io.NotConnectedException, com.sun.star.io.IOException {
        _check();
        return position - readPosition;
    }

    public void closeInput() throws com.sun.star.io.NotConnectedException, com.sun.star.io.IOException {
        closed = true;
    }

    public int readBytes(byte[][] values, int param) throws com.sun.star.io.NotConnectedException, com.sun.star.io.BufferSizeExceededException, com.sun.star.io.IOException {
        _check();
        try {
            int remain = (int)(position - readPosition);
            if (param > remain) param = remain;
            /* ARGH!!! */
            if (values[0] == null){
                values[0] = new byte[param];
                // System.err.println("allocated new buffer of "+param+" bytes");
            }
            System.arraycopy(buffer, readPosition, values[0], 0, param);
            // System.err.println("readbytes() -> "+param);
            readPosition += param;
            return param;
        } catch (ArrayIndexOutOfBoundsException ae) {
            // System.err.println("readbytes() -> ArrayIndexOutOfBounds");
            ae.printStackTrace();
            throw new com.sun.star.io.BufferSizeExceededException("buffer overflow");
        } catch (Exception e) {
            // System.err.println("readbytes() -> Exception: "+e.getMessage());
            e.printStackTrace();
            throw new com.sun.star.io.IOException("error accessing buffer");
        }
    }

    public int readSomeBytes(byte[][] values, int param) throws com.sun.star.io.NotConnectedException, com.sun.star.io.BufferSizeExceededException, com.sun.star.io.IOException {
        // System.err.println("readSomebytes()");
        return readBytes(values, param);
    }

    public void skipBytes(int param) throws com.sun.star.io.NotConnectedException, com.sun.star.io.BufferSizeExceededException, com.sun.star.io.IOException {
        // System.err.println("skipBytes("+param+")");
        _check();
        if (param > (position - readPosition))
            throw new com.sun.star.io.BufferSizeExceededException("buffer overflow");
        readPosition += param;
    }


    // Implementation of XSeekable

    public long getLength() throws com.sun.star.io.IOException {
        // System.err.println("getLength() -> "+m_length);
        if (buffer != null) return position;
        else throw new com.sun.star.io.IOException("no bytes");
    }

    public long getPosition() throws com.sun.star.io.IOException {
        // System.err.println("getPosition() -> "+m_pos);
        if (buffer != null) return readPosition;
        else throw new com.sun.star.io.IOException("no bytes");
    }

    public void seek(long param) throws com.sun.star.lang.IllegalArgumentException, com.sun.star.io.IOException {
        // System.err.println("seek("+param+")");
        if (buffer != null) {
            if (param < 0 || param > position) throw new com.sun.star.lang.IllegalArgumentException("invalid seek position");
            else readPosition = (int)param;
        } else throw new com.sun.star.io.IOException("no bytes");
     }

    // Implementation of XStream
    public XInputStream getInputStream() { return this; }

    public XOutputStream getOutputStream() { return this; }

    // Get the buffer
    public byte[] getBuffer() { return buffer; }

}
