/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Johan Boye, KTH, 2018
 */

package ir;

import java.io.*;
import java.util.*;
import java.nio.charset.*;
import java.lang.StringBuilder;

/*
 *   Implements an inverted index as a hashtable on disk.
 *
 *   Both the words (the dictionary) and the data (the postings list) are
 *   stored in RandomAccessFiles that permit fast (almost constant-time)
 *   disk seeks.
 *
 *   When words are read and indexed, they are first put in an ordinary,
 *   main-memory HashMap. When all words are read, the index is committed
 *   to disk.
 */
public class PersistentHashedIndex implements Index {

    /** The directory where the persistent index files are stored. */
    public static final String INDEXDIR = "./index";

    /** The dictionary file name */
    public static final String DICTIONARY_FNAME = "dictionary";

    /** The dictionary file name */
    public static final String DATA_FNAME = "data";

    /** The terms file name */
    public static final String TERMS_FNAME = "terms";

    /** The doc info file name */
    public static final String DOCINFO_FNAME = "docInfo";

    /** The dictionary hash table on disk can fit this many entries. */
    public static final long TABLESIZE = 611953L;

    /** The dictionary hash table is stored in this file. */
    RandomAccessFile dictionaryFile;

    /** The data (the PostingsLists) are stored in this file. */
    RandomAccessFile dataFile;

    /** Pointer to the first free memory cell in the data file. */
    long free = 0L;

    /** The cache as a main-memory hash map. */
    HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();


    // ===================================================================

    /**
     *   A helper class representing one entry in the dictionary hashtable.
     */
    public class Entry {
        //
        //  YOUR CODE HERE
        //
        String term;
        long ptr;
        int size;

        //Constructor
        public Entry(String term, long ptr, int size){
          this.term = term;
          this.ptr = ptr;
          this.size = size;
        }

        public Entry(long ptr, int size){
          this.ptr = ptr;
          this.size = size;
        }
    }


    // ==================================================================


    /**
     *  Constructor. Opens the dictionary file and the data file.
     *  If these files don't exist, they will be created.
     */
    public PersistentHashedIndex() {
        try {
            dictionaryFile = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME, "rw" );
            dataFile = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME, "rw" );
        } catch ( IOException e ) {
            e.printStackTrace();
        }

        try {
            readDocInfo();
        } catch ( FileNotFoundException e ) {
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     *  Writes data to the data file at a specified place.
     *
     *  @return The number of bytes written.
     */
    int writeData( String dataString, long ptr ) {
        try {
            dataFile.seek( ptr );
            byte[] data = dataString.getBytes();
            dataFile.write( data );
            return data.length;
        }  catch ( IOException e ) {
            e.printStackTrace();
            return -1;
        }
    }


    /**
     *  Reads data from the data file
     */
    String readData( long ptr, int size ) {
        try {
            dataFile.seek( ptr );
            byte[] data = new byte[size];
            dataFile.readFully( data );
            return new String(data);
        }  catch ( IOException e ) {
            e.printStackTrace();
            return null;
        }
    }


    // ==================================================================
    //
    //  Reading and writing to the dictionary file.

    /*
     *  Writes an entry to the dictionary hash table file.
     *
     *  @param entry The key of this entry is assumed to have a fixed length
     *  @param ptr   The place in the dictionary file to store the entry
     */

    void writeEntry( Entry entry, long hash ) {
       try {
            String rev = new StringBuilder(entry.term).reverse().toString();
            long h2 = hash(rev);
            h2 = (h2%900);
           dictionaryFile.seek(hash*1000 + h2);
           dictionaryFile.writeChar(entry.term.charAt(0));
           dictionaryFile.writeLong(entry.ptr);
           dictionaryFile.writeInt(entry.size);
         }
         catch ( IOException e ) {
             e.printStackTrace();
         }
     }


  /**
     *  Reads an entry from the dictionary file.
     *
     *  @param ptr The place in the dictionary file where to start reading.
     */
    Entry readEntry(String term, long hash){

        try {
          String rev = new StringBuilder(term).reverse().toString();
          long h2 = hash(rev);
          h2 = (h2%900);
            dictionaryFile.seek(hash*1000 + h2);
            if(dictionaryFile.readChar() == (term.charAt(0))){
              long ptr = dictionaryFile.readLong();
              int size = dictionaryFile.readInt();
              Entry e = new Entry(ptr, size);
              return e;
            }
            else{return null;}
        }
        catch ( IOException e ) {
            e.printStackTrace();
            return null;
        }
    }


    // ==================================================================

    /**
     *  Writes the document names and document lengths to file.
     *
     * @throws IOException  { exception_description }
     */
    private void writeDocInfo() throws IOException {
        FileOutputStream fout = new FileOutputStream( INDEXDIR + "/docInfo" );
        for (Map.Entry<Integer,String> entry : docNames.entrySet()) {
            Integer key = entry.getKey();
            String docInfoEntry = key + ";" + entry.getValue() + ";" + docLengths.get(key) + "\n";
            fout.write(docInfoEntry.getBytes());
        }
        fout.close();
    }


    /**
     *  Reads the document names and document lengths from file, and
     *  put them in the appropriate data structures.
     *
     * @throws     IOException  { exception_description }
     */
    private void readDocInfo() throws IOException {
        File file = new File( INDEXDIR + "/docInfo" );
        FileReader freader = new FileReader(file);
        try (BufferedReader br = new BufferedReader(freader)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(";");
                docNames.put(new Integer(data[0]), data[1]);
                docLengths.put(new Integer(data[0]), new Integer(data[2]));
            }
        }
        freader.close();
    }


    /**
     *  Write the index to files.
     */
    public void writeIndex() {
        int collisions = 0;
        ArrayList<Long> hashes = new ArrayList<Long>();
        try {
            // Write the 'docNames' and 'docLengths' hash maps to a file
            writeDocInfo();

            // Write the dictionary and the postings list

            //
            //  YOUR CODE HERE
            //

            // Go through all terms in the index
            long ptr = free;
            for(String term: index.keySet()){
                String encodedPostings = encode(index.get(term));
                int size = writeData(encodedPostings, ptr);
                Entry entry = new Entry(term, ptr, size);
                long hash = hash(term);
                if(hashes.contains(hash)){collisions++;}
                else{hashes.add(hash);}
                writeEntry(entry, hash);
                ptr += size;
            }

            //closefiles();

        }
        catch ( IOException e ) {
            e.printStackTrace();
        }
        System.err.println( collisions + " collisions." );
    }


    // ==================================================================


    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String term ) {
        //
        //  REPLACE THE STATEMENT BELOW WITH YOUR CODE
        //

        long hash = hash(term);
        Entry entry = readEntry(term, hash);
        if(entry == null ){
          return null;
        }
        else{
          String data = readData(entry.ptr, entry.size);
          PostingsList pl = decode(data);
          return pl;
        }

    }


    /**
     *  Inserts this token in the main-memory hashtable.
     */
    public void insert( String token, int docID, int offset ) {
      //if token is not in hashmap, make new postings list
      if(!index.containsKey(token)){index.put(token, new PostingsList());}

      //add new entry
      index.get(token).insert(docID, 1.0, offset);
    }

    public long hash(String str){
      long hash = 7;
      for(int i = 0; i < str.length(); i++){
        hash = hash*31 + str.charAt(i);
        hash = (hash % TABLESIZE);
      }
      return hash;
  }

  /*
  "Entry: [ [ int double [ int int int ] ] [ [ int double [ int int int ] ] [ [ int
  double [ int int int ] ]  "
  */
  public String encode(PostingsList pl){
    StringBuilder sb = new StringBuilder();
    ArrayList<PostingsEntry> list = pl.list;
    ArrayList<Integer> containedDocs = pl.containedDocs;
    sb.append("[ ");
    sb.append("[ ");
    for(PostingsEntry entry: list){
      sb.append("[ ");
      sb.append(entry.docID);
      sb.append(" ");
      sb.append(entry.score);
      sb.append(" ");
      sb.append("[ ");
      for(int pos: entry.positions){
        sb.append(pos);
        sb.append(" ");
      }
      sb.append("] ");
      sb.append("] ");

    }
    sb.append("] ");
    sb.append("[ ");
    for(int doc: containedDocs){
      sb.append(doc);
      sb.append(" ");
    }
    sb.append("] ");
    sb.append("]");

    return sb.toString();
  }



  public PostingsList decode(String str){
    //go through list and create objects
    PostingsList pl = new PostingsList();


    String[] split = str.split("\\s+");
    if(!split[0].equals("[")){return null;}
    boolean flag = true;
    int i = 3;
    while(flag) {
      PostingsEntry entry = new PostingsEntry(Integer.parseInt(split[i]), Double.parseDouble(split[i+1]));
      i += 3;
      while(!split[i].equals("]")){
        entry.positions.add(Integer.parseInt(split[i]));
        i++;
      }
      pl.list.add(entry);
      if(split[i+2].equals("]")){
        i+=4;
        flag = false;
      }
      else{
        i+=3;
      }
    }

    while(!split[i].equals("]")){
      pl.containedDocs.add(Integer.parseInt(split[i]));
      i++;
    }
    return pl;
  }
    /**
     *  Write index to file after indexing is done.
     */
    public void cleanup() {
        System.err.println( index.keySet().size() + " unique words" );
        System.err.print( "Writing index to disk..." );
        writeIndex();
        System.err.println( "done!" );
    }

    public void closefiles(){
      try {
        dataFile.close();
        dictionaryFile.close();
        dictionaryFile = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME, "r" );
        dataFile = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME, "r" );
      } catch ( IOException e ) {
          e.printStackTrace();
      }
    }
}
