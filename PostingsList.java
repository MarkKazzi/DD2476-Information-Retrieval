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


    /** Number of postings in this list. */
    public int size() {
    return list.size();
    }

    /** Returns the ith posting. */
    public PostingsEntry get( int i ) {
    return list.get( i );
    }

    //
    //  YOUR CODE HERE
    //

    public void insert(int docID, double score, int offset){
      boolean contained = false;
        for(PostingsEntry entry: list){
        if(entry.docID == docID){
          contained = true;
          entry.addOffset(offset);
          break;
        }
      }


      if(!contained){
        PostingsEntry entry = new PostingsEntry(docID, score);
        list.add(entry);
        entry.addOffset(offset);
      }


    }

  /*    public void insert(int docID, double score){
        if(!list.isEmpty()){
          if(list.get(list.size()-1).docID < docID){
            PostingsEntry entry = new PostingsEntry(docID, score);
            list.add(entry);
          }
        }
    }*/
}
