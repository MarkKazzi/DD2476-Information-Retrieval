/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Johan Boye, 2017
 */

package ir;
import java.util.ArrayList;
import java.util.Collection;

/**
 *  Searches an index for results of a query.
 */
public class Searcher {

    /** The index to be searched by this Searcher. */
    Index index;

    /** The k-gram index to be searched by this Searcher */
    KGramIndex kgIndex;

    /** Constructor */
    public Searcher( Index index, KGramIndex kgIndex ) {
        this.index = index;
        this.kgIndex = kgIndex;
    }

    /**
     *  Searches the index for postings matching the query.
     *  @return A postings list representing the result of the query.
     */
    public PostingsList search( Query query, QueryType queryType, RankingType rankingType ) {

      if(query.queryterm.size() == 1){
        String token = query.queryterm.get(0).term;
        return index.getPostings(token);
      }
      else{

        //terms = query.queryterm;
        //while(i < terms.size()){

        //PostingsList p1 = index.getPostings(query.queryterm.get(0).term);
        //PostingsList p2 = index.getPostings(query.queryterm.get(1).term);
        //ArrayList<PostingsList> terms = terms(query);
        //PostingsList result = terms.get(0);
        
        ArrayList<PostingsList> terms = new ArrayList<PostingsList>();
        for(int i=0; i<query.queryterm.size(); i++){
          terms.add(index.getPostings(query.queryterm.get(i).term));
        }

        PostingsList result = new PostingsList();
        result = terms.get(0);

        for(int i = 1; i < terms.size(); i++){
          result = intersect(result, terms.get(i));
        }
      return result;
      }
}
      public PostingsList intersect (PostingsList p1, PostingsList p2){
        int i = 0;
        int j = 0;
        PostingsList answer = new PostingsList();
        ArrayList<PostingsEntry> list1 = p1.list;
        ArrayList<PostingsEntry> list2 = p2.list;

        while((i < list1.size()) && (j < list2.size())){
            PostingsEntry entry1 = list1.get(i);
            PostingsEntry entry2 = list2.get(j);
            if(entry1.docID == entry2.docID){
              answer.list.add(entry1);
              i++;
              j++;
            }
            else if(entry1.docID < entry2.docID){
              i++;
            }
            else{j++;}
        }
        return answer;
      }


      public ArrayList<PostingsList> terms(Query query) {
        ArrayList<PostingsList> sortedTerms = new ArrayList<PostingsList>();
        sortedTerms.add(index.getPostings(query.queryterm.get(0).term));
        for(int i=0; i < query.queryterm.size(); i++){
          for(int j =0; j< sortedTerms.size(); j++){
            if(index.getPostings(query.queryterm.get(i).term).size() < index.getPostings(query.queryterm.get(j).term).size()){
          sortedTerms.add(j, index.getPostings(query.queryterm.get(i).term));
          sortedTerms.add(j+1, index.getPostings(query.queryterm.get(j).term));
          }
        }
      }

        return sortedTerms;
    }



}
