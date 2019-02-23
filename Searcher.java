
/*
*   This file is part of the computer assignment for the
*   Information Retrieval course at KTH.
*
*   Johan Boye, 2017
*/

package ir;
import java.util.*;
import java.lang.Math;
import java.io.*;

/**
*  Searches an index for results of a query.
*/
public class Searcher {

  /** The index to be searched by this Searcher. */
  Index index;

  /** The k-gram index to be searched by this Searcher */
  KGramIndex kgIndex;

// Map from document name to pagerank score
  HashMap<String, Double> pageranks = new HashMap<String, Double>();

  double PRInfluence = 0.9999;

  PostingsList pagerankPostings = new PostingsList();
  PostingsList tfIdfPostings =  new PostingsList();


  /** Constructor */
  public Searcher( Index index, KGramIndex kgIndex ) {
    this.index = index;
    this.kgIndex = kgIndex;
  }

  /**
   *  Searches the index for postings matching the query.
   *  @return A postings list representing the result of the query.
   */
  public PostingsList search( Query query, QueryType queryType, RankingType rankingType) {
    if(queryType == queryType.INTERSECTION_QUERY){return intersection_search(query);}
    else if (queryType == queryType.PHRASE_QUERY){return phrase_search(query);}
    else if (queryType == queryType.RANKED_QUERY){return ranked_search(query, rankingType);}
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
      ArrayList<Integer> finalresult = new ArrayList<Integer>();
      for(int i = 0; i < commonDocs.size(); i++){
        int docID = commonDocs.get(i);
        ArrayList<Integer> positions1 = null;
        for(int j = 0; j < query.queryterm.size() -1; j++){
          int posInOffset1 = 0;
          int posInOffset2 = 0;
          if(positions1 == null){
            for(int k = 0; k < index.getPostings(query.queryterm.get(j).term).list.size(); k++ ){
              if(index.getPostings(query.queryterm.get(j).term).list.get(k).docID == docID){ //optimera här
                posInOffset1 = k;
              }
            }
            positions1 = index.getPostings(query.queryterm.get(j).term).list.get(posInOffset1).positions;
          }
          for(int k = 0; k < index.getPostings(query.queryterm.get(j+1).term).list.size(); k++ ){
            if(index.getPostings(query.queryterm.get(j+1).term).list.get(k).docID == docID){
              posInOffset2 = k;
          }
        }
        ArrayList<Integer> positions2 = index.getPostings(query.queryterm.get(j+1).term).list.get(posInOffset2).positions;
        positions2 = phrase_find(positions1, positions2);
        if (positions2.isEmpty()){
          break;
        }
        else if(j== query.queryterm.size() -2){
          finalresult.add(docID);
        }
        positions1 = positions2;
      }
    }
    for(int f: finalresult){
      answer.insert(f,0,0);
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
      else if(entry1.docID < entry2.docID){i++;}
      else{j++;}
    }
    return answer;
  }

  public ArrayList<Integer> phrase_find(ArrayList<Integer> positions1, ArrayList<Integer> positions2){
    ArrayList<Integer> newpositions2 = new ArrayList<Integer>();
    for(int i = 0; i < positions1.size(); i++){
      for(int j = 0; j < positions2.size(); j++){
        if((positions2.get(j) - positions1.get(i)) > 1){
          break;
        }
        if((positions2.get(j)-positions1.get(i)) == 1 ){
          newpositions2.add(positions2.get(j));
        }
      }
    }
    return newpositions2;
  }

  public PostingsList ranked_search(Query query, RankingType rankingType){
    if(rankingType == RankingType.TF_IDF){
      PostingsList ret = ranked_search_tf_idf(query);
      Collections.sort(ret.list);
      return ret;
    }
    if(rankingType == RankingType.PAGERANK){
      PostingsList ret = ranked_search_pagerank(query);
      Collections.sort(ret.list);
      return ret;
    }
    else{
      pagerankPostings = ranked_search_pagerank(query);
      tfIdfPostings = ranked_search_tf_idf(query);
      PostingsList ret = new PostingsList();
      for(int i = 0; i < tfIdfPostings.list.size(); i++){
        PostingsEntry entry = tfIdfPostings.list.get(i);
        double tfIdf = entry.score;
        double pagerank = pagerankPostings.list.get(i).score;
        double combinedScore = ((double)1-PRInfluence)*tfIdf + PRInfluence*pagerank;
        entry.score = combinedScore;
        ret.list.add(entry);
      }
      Collections.sort(ret.list);
      return ret;
    }
  }

  public PostingsList ranked_search_tf_idf(Query query){

    //isolate unique terms in the query and number
    //of occurences of each term in the query
    ArrayList<String> uniqueQueries = new ArrayList<String>();
    ArrayList<Integer> tfQuery = new ArrayList<Integer>();
    for(int i = 0; i < query.queryterm.size(); i++){
      String term = query.queryterm.get(i).term;
      if(!uniqueQueries.contains(term)){
        uniqueQueries.add(term);
        tfQuery.add(i, 1);
      }
      else{
        int index = uniqueQueries.indexOf(term);
        tfQuery.add(i, (tfQuery.get(index) + 1));
      }
    }

    //Find idf for each unique term in the query
    int N = index.docNames.size();
    ArrayList<Double> idfs = new ArrayList<Double>();
    for(String term: uniqueQueries){
      int df = index.getPostings(term).list.size();
      double idf = Math.log(N/df);
      idfs.add(idf);
    }

    //Find tf_idf vector for query
    ArrayList<Double> Q = new ArrayList<Double>();
    int lenQ = query.queryterm.size();
    for(int i = 0; i < uniqueQueries.size(); i++){
      int tf = tfQuery.get(i);
      double idf = idfs.get(i);
      double tfIdf = tf * (idf/lenQ);
      Q.add(tfIdf);
    }

    //Combine all unique documents containing at leas one of the query terms
    // and merge those containing more than one term
    PostingsList all = new PostingsList();
    ArrayList<Integer> docIDs = new ArrayList<Integer>();
    for(String term: uniqueQueries){
      for(PostingsEntry entry: index.getPostings(term).list){
        int docID = entry.docID;
        if(!docIDs.contains(docID)){
          docIDs.add(docID);
          entry.overlaps.put(term, entry.positions);
          all.list.add(entry);
        }
        else{
          for(PostingsEntry old: all.list){
            if(old.docID == docID){
              old.overlaps.put(term, entry.positions);
              break;
            }
          }
        }
      }
    }

    //Compare tf_idf vector of query with tf_idf vector of each
    //doc and assign scores
    PostingsList ret = new PostingsList();
    for(PostingsEntry entry: all.list){
      PostingsEntry e = new PostingsEntry(entry.docID, tf_idf_score(entry, Q, idfs, uniqueQueries));
      ret.list.add(e);
    }
    return ret;
  }

  public PostingsList ranked_search_pagerank(Query query){
    //isolate unique terms in the query
    ArrayList<String> uniqueQueries = new ArrayList<String>();
      for(int i = 0; i < query.queryterm.size(); i++){
      String term = query.queryterm.get(i).term;
      if(!uniqueQueries.contains(term)){
        uniqueQueries.add(term);
      }
    }

    //Combine all UNIQUE documents containing at leas one of the query terms
    PostingsList all = new PostingsList();
    ArrayList<Integer> docIDs = new ArrayList<Integer>();
    for(String term: uniqueQueries){
      for(PostingsEntry entry: index.getPostings(term).list){
        int docID = entry.docID;
        if(!docIDs.contains(docID)){
          docIDs.add(docID);
          all.list.add(entry);
        }
      }
    }

    load_pageranks();

    //Compare to pagerank file and assign scores for each document
    PostingsList ret = new PostingsList();
    for(PostingsEntry entry: all.list){
      int docID = entry.docID;
      String title = index.docNames.get(docID);
      int i = title.lastIndexOf("/"); //ignore path name, need only doc name
      double rank = pageranks.get(title.substring(i+1, title.length()));
      PostingsEntry e = new PostingsEntry(entry.docID, rank);
      ret.list.add(e);
    }
    return ret;
  }

  public void load_pageranks(){
    String f = "pagerank";
    BufferedReader reader = null;
    try{
      reader = new BufferedReader(new FileReader(f));
      String line = null;
      while((line = reader.readLine()) != null){
        String[] strings = line.split(";");
        pageranks.put(strings[0] , Double.valueOf(strings[1]));
      }
      reader.close();
    }
    catch (Exception e){
      e.printStackTrace();
    }
  }

  public double tf_idf_score(PostingsEntry entry, ArrayList<Double> Q,ArrayList<Double> idfs, ArrayList<String> uniqueQueries){

  //Compute tf_idf vector for this document
    ArrayList<Double> D = new ArrayList<Double>();
      for(int i = 0; i < uniqueQueries.size(); i++){
      String term = uniqueQueries.get(i);
      if(!entry.overlaps.containsKey(term)){
        D.add(0.0);
      }
      else{
        int tf = entry.overlaps.get(term).size();
        int lenD =  index.docLengths.get(entry.docID);
        double idf = idfs.get(i);
        double tfIdfD = tf *(idf/lenD);
        D.add(tfIdfD);
      }
    }
    return cosine_similarity(Q, D);
  }

  public double cosine_similarity(ArrayList<Double> Q, ArrayList<Double> D){
    int len = Q.size();
    double sum = 0;
    for(int i = 0; i < len; i++){
      sum += Q.get(i) * D.get(i);
    }
    double score = sum/len;
    return score;
  }
}
