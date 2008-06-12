package er.indexing.eof;

import java.io.IOException;

import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;

import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSData;
import com.webobjects.foundation.NSMutableData;
import com.webobjects.foundation.NSMutableRange;
import com.webobjects.foundation.NSTimestamp;

public class ERIFile extends _ERIFile {

    @SuppressWarnings("unused")
    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ERIFile.class);

    public static final ERIFileClazz clazz = new ERIFileClazz();
    public static class ERIFileClazz extends _ERIFile._ERIFileClazz {
        /* more clazz methods here */
    }

    public interface Key extends _ERIFile.Key {}

    private class EOFIndexOutput extends IndexOutput {

        long filePointer = 0;
        NSMutableData data;
        boolean dirty = false;
        
        public EOFIndexOutput(NSData contentData) {
            data = new NSMutableData(contentData);
        }

        private NSMutableData data() {
            return data;
        }

        @Override
        public void close() throws IOException {
            flush();
        }

        @Override
        public void flush() throws IOException {
            if(dirty) {
                setContentData(data());
                editingContext().saveChanges();
            }
            dirty = false;
        }

        @Override
        public long getFilePointer() {
            return filePointer;
        }

        @Override
        public long length() {
            return data().length();
        }

        @Override
        public void seek(long l) throws IOException {
            assureLength(l);
            filePointer = l;
        }
        
        private void assureLength(long l) {
            if(length() < l) {
                data().setLength((int) l);
                dirty = true;
            }
        }
        
        @Override
        public void writeByte(byte byte0) throws IOException {
            assureLength(getFilePointer()+1);
            data().bytesNoCopy(new NSMutableRange((int)filePointer++, 1))[0] = byte0;
            dirty = true;
        }

        @Override
        public void writeBytes(byte[] abyte0, int offset, int len) throws IOException {
            assureLength(getFilePointer()+len);
            byte[] buf = data().bytesNoCopy(new NSMutableRange((int)filePointer, len));
            System.arraycopy(abyte0, offset, buf, 0, len);
            filePointer += len;
            dirty = true;
        }
    }
    
    private class EOFIndexInput extends IndexInput {

        long filePointer = 0;
        NSData data;
        
        public EOFIndexInput(NSData contentData) {
            data = contentData;
        }

        private NSData data() {
            return data;
        }

        @Override
        public void close() throws IOException {
            filePointer = 0;
        }

        @Override
        public long getFilePointer() {
            return filePointer;
        }

        @Override
        public long length() {
            return data().length();
        }
        
        private void assureLength(long len) throws IOException {
            if(len > length()) {
                throw new IOException("Not enough data: " + len + " vs " + length());
            }
        }

        @Override
        public byte readByte() throws IOException {
            assureLength(filePointer+1);
            return data().bytes((int)filePointer++, 1)[0];
        }

        @Override
        public void readBytes(byte[] abyte0, int offset, int len) throws IOException {
            assureLength(filePointer+len);
            System.arraycopy(data().bytesNoCopy(new NSMutableRange((int)filePointer, len)), 0, abyte0, offset, len);
            filePointer += len;
        }

        @Override
        public void seek(long l) throws IOException {
            assureLength(l);
            filePointer = l;
        }
    }
    
    public void init(EOEditingContext ec) {
        super.init(ec);
        setContentData(new NSData());
    }
    
    private void setContentData(NSData data) {
        setLastModified(new NSTimestamp());
        setLength((long)data.length());
        content().setContent(data);
    }

    private NSData contentData() {
        return content().content();
    }

    public void touch() {
        setLastModified(new NSTimestamp());
        editingContext().saveChanges();
    }

    public IndexInput openInput() {
        return new EOFIndexInput(contentData());
    }

    public long timestamp() {
        return lastModified().getTime();
    }

    public IndexOutput createOutput() {
        return new EOFIndexOutput(contentData());
    }
}
