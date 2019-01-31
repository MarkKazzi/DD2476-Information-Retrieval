/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Johan Boye, 2017
 */

package ir;

import java.util.ArrayList;

public class PostingsList {

    /** The postings list */
    public ArrayList<PostingsEntry> list = new ArrayList<PostingsEntry>();

    public ArrayList<Integer> containedDocs = new ArrayList<Integer>();


    /** Number of postings in this list. */
    public int size() {
    return list.size();
    }

    /** Returns the ith posting. */
    public PostingsEntry get( int i ) {
    return list.get( i );
    }

    public void insert(int docID, double score, int offset){
        if(containedDocs.contains(docID)){  //O(n), do better?
          int i = containedDocs.indexOf(docID);
          PostingsEntry entry = list.get(i);
          entry.addOffset(offset);
        }
        else{
          PostingsEntry entry = new PostingsEntry(docID, score);
          list.add(entry);
          entry.addOffset(offset);
          containedDocs.add(docID);
        }
    }
}
