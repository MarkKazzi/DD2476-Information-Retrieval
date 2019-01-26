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

      if(queryType == queryType.INTERSECTION_QUERY){return intersection_search(query);}
      else if (queryType == queryType.PHRASE_QUERY){return phrase_search(query);}
      else{return null;}
    }

      public PostingsList intersection_search(Query query){
        if(query.queryterm.size() == 1){
          String token = query.queryterm.get(0).term;
          return index.getPostings(token);
        }
        else{
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

      public PostingsList phrase_search(Query query){

        if(query.queryterm.size() == 1){
          String token = query.queryterm.get(0).term;
          return index.getPostings(token);
        }
        else{
            PostingsList answer = new PostingsList();

            ArrayList<PostingsList> terms = new ArrayList<PostingsList>();
            for(int i=0; i<query.queryterm.size(); i++){
              terms.add(index.getPostings(query.queryterm.get(i).term));
            }

            PostingsList result = new PostingsList();
            result = terms.get(0);

            for(int i = 1; i < terms.size(); i++){
              result = intersect(result, terms.get(i));
            }
              //docIDs of all documents intersecting the query terms
              ArrayList<Integer> commonDocs = new ArrayList<Integer>();
              for(int i = 0; i < result.list.size(); i++){
                commonDocs.add(result.get(i).docID);
              }



              for(int i = 0; i < commonDocs.size(); i++){
                int docID = commonDocs.get(i);
                int posInOffset1 = 0;
                int posInOffset2 = 0;

                boolean consistentFind = true;
                ArrayList<Integer> input = new ArrayList<Integer>();

                for(int j = 0; j < query.queryterm.size() -1; j++){
                  for(int k = 0; k < index.getPostings(query.queryterm.get(j).term).list.size(); k++ ){
                    if(index.getPostings(query.queryterm.get(j).term).list.get(k).docID == docID){
                        posInOffset1 = k;
                    }
                  }
                  for(int k = 0; k < index.getPostings(query.queryterm.get(j+1).term).list.size(); k++ ){
                    if(index.getPostings(query.queryterm.get(j+1).term).list.get(k).docID == docID){
                        posInOffset2 = k;
                    }
                  }
                  ArrayList<Integer> positions1 = index.getPostings(query.queryterm.get(j).term).list.get(posInOffset1).positions;
                  ArrayList<Integer> positions2 = index.getPostings(query.queryterm.get(j+1).term).list.get(posInOffset2).positions;

                  if(j == 0){
                    input = positions1;
                  }
                  input = phrase_find(input, positions2);
                  if(input.isEmpty()){
                    consistentFind = false;
                    break;
                  }

                }
                if(consistentFind){
                  answer.list.add(result.get(i));

                }
                else{
                  consistentFind = true;
                }
              }
              return answer;
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

        public ArrayList<Integer> phrase_find(ArrayList<Integer> positions1, ArrayList<Integer> positions2){
          ArrayList<Integer> previousPositions = new ArrayList<Integer>();

          for(int i = 0; i < positions1.size(); i++){
            for(int j = 0; j < positions2.size(); j++){
              if((positions2.get(j) - positions1.get(i)) > 1){break;}
              if((positions2.get(j)-positions1.get(i)) == 1 )
              {
                  previousPositions.add(j);
              }
            }
          }
            return previousPositions;
        }
}
