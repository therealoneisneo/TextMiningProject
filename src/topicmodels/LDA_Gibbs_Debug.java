package topicmodels;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import structures.MyPriorityQueue;
import structures._ChildDoc;
import structures._Corpus;
import structures._Doc;
import structures._ParentDoc;
import structures._RankItem;
import structures._SparseFeature;
import structures._Stn;
import structures._Word;
import utils.Utils;

public class LDA_Gibbs_Debug extends LDA_Gibbs{
	Random m_rand;
	int m_burnIn; // discard the samples within burn in period
	int m_lag; // lag in accumulating the samples
	
	double[] m_topicProbCache;
	
	//all computation here is not in log-space!!!
	public LDA_Gibbs_Debug(int number_of_iteration, double converge, double beta,
			_Corpus c, double lambda, 
			int number_of_topics, double alpha, double burnIn, int lag) {
		super( number_of_iteration,  converge,  beta,
			 c,  lambda, number_of_topics,  alpha,  burnIn,  lag);
		
		m_rand = new Random();
		m_burnIn = (int) (burnIn * number_of_iteration);
		m_lag = lag;
		
		m_topicProbCache = new double[number_of_topics];
	}
	
	protected void initialize_probability(Collection<_Doc> collection) {
		for(int i=0; i< number_of_topics; i++)
			Arrays.fill(word_topic_sstat[i], d_beta);
		Arrays.fill(m_sstat, d_beta*vocabulary_size);
		
		for(_Doc d: collection){
			if(d instanceof _ParentDoc) {
				for(_Stn stnObj: d.getSentences()){
					stnObj.setTopicsVct(number_of_topics);
				}	
				d.setTopics4Gibbs(number_of_topics, d_alpha);
			}else if(d instanceof _ChildDoc){
				((_ChildDoc) d).setTopics4Gibbs_LDA(number_of_topics, d_alpha);
			}
			
			
			for(_Word w:d.getWords()) {
				word_topic_sstat[w.getTopic()][w.getIndex()] ++;
				m_sstat[w.getTopic()] ++;
			}
		}
		
		imposePrior();
	};
	
	protected void collectStats(_Doc d) {
		for(int k=0; k<this.number_of_topics; k++)
			d.m_topics[k] += d.m_sstat[k] + d_alpha;
		if(d instanceof _ParentDoc){
			((_ParentDoc) d).collectTopicWordStat();
		}
	}
	
	protected double wordByTopicProb(int tid, int wid){
		return word_topic_sstat[tid][wid]/m_sstat[tid];
	}
	
	protected double topicInDocProb(int tid, _Doc d){
		return (d_alpha+d.m_sstat[tid]);
	}
	
	protected void finalEst(){
		super.finalEst();
	}
	
	protected void estThetaInDoc(_Doc d) {
		super.estThetaInDoc(d);
		if(d instanceof _ParentDoc){
			estParentStnTopicProportion((_ParentDoc)d);
//			((_ParentDoc)d).estStnTheta();
		}
	}
	
	public void estParentStnTopicProportion(_ParentDoc pDoc){
		for(_Stn stnObj : pDoc.getSentences() ){
			estStn(stnObj, pDoc);
		}
	}
	
	public void estStn(_Stn stnObj,  _ParentDoc d){
		int i=0;
		initStn(stnObj);
		do{
			calculateStn_E_step(stnObj, d);
			if(i>m_burnIn && i%m_lag == 0){
				collectStnStats(stnObj, d);
			}
			
		}while(++i<number_of_iteration);
		
		Utils.L1Normalization(stnObj.m_topics);
	}
	
	public void initStn(_Stn stnObj){
		stnObj.setTopicsVct(number_of_topics);
	}
	
	public void calculateStn_E_step( _Stn stnObj, _ParentDoc d){
		stnObj.permuteStn();
		
		double normalizedProb = 0;
		int wid, tid;
		for(_Word w: stnObj.getWords()){
			wid = w.getIndex();
			tid = w.getTopic();
	
			stnObj.m_topicSstat[tid] --;
		
			normalizedProb = 0;
			
			double pWordTopic = 0;
			
			for(tid=0; tid<number_of_topics; tid++){
				pWordTopic = wordByTopicProb(tid, wid);
				double pTopic = parentTopicInStnProb(tid, stnObj, d);
				
				m_topicProbCache[tid] = pWordTopic*pTopic;
				normalizedProb += m_topicProbCache[tid];
			}
			
			normalizedProb *= m_rand.nextDouble();
			for(tid=0; tid<m_topicProbCache.length; tid++){
				normalizedProb -= m_topicProbCache[tid];
				if(normalizedProb <= 0)
					break;
			}
			
			if(tid==m_topicProbCache.length)
				tid --;
			

			w.setTopic(tid);
			stnObj.m_topicSstat[tid] ++;
			
		}
		
	}
	
	public double parentTopicInStnProb(int tid, _Stn stnObj, _ParentDoc d){
		return (d_alpha + d.m_topics[tid]+stnObj.m_topicSstat[tid])/(number_of_topics*d_alpha+1+stnObj.getLength());
	}
	
	public void collectStnStats(_Stn stnObj, _ParentDoc d){
		for(int k=0; k<number_of_topics; k++){
			stnObj.m_topics[k] += stnObj.m_topicSstat[k]+d_alpha+d.m_topics[k];
		}
	}
	
	public void crossValidation(int k) {
		m_trainSet = new ArrayList<_Doc>();
		m_testSet = new ArrayList<_Doc>();
		
		double[] perf = null;
		
		_Corpus parentCorpus = new _Corpus();
		ArrayList<_Doc> docs = m_corpus.getCollection();
		ArrayList<_ParentDoc> parentDocs = new ArrayList<_ParentDoc>();
		for(_Doc d: docs){
			if(d instanceof _ParentDoc){
				parentCorpus.addDoc(d);
				parentDocs.add((_ParentDoc) d);
			}
		}
		
		System.out.println("size of parent docs\t"+parentDocs.size());
		
		parentCorpus.setMasks();
		if(m_randomFold==true){
			perf = new double[k];
			parentCorpus.shuffle(k);
			int[] masks = parentCorpus.getMasks();
			
			for(int i=0; i<k; i++){
				for(int j=0; j<masks.length; j++){
					if(masks[j] == i){
						m_testSet.add(parentDocs.get(j));
					}else {
						m_trainSet.add(parentDocs.get(j));
						for(_ChildDoc d: parentDocs.get(j).m_childDocs){
							m_trainSet.add(d);
						}
					}
					
				}
				
//				writeFile(i, m_trainSet, m_testSet);
				System.out.println("Fold number "+i);
				System.out.println("Train Set Size "+m_trainSet.size());
				System.out.println("Test Set Size "+m_testSet.size());

				long start = System.currentTimeMillis();
				EM();
				perf[i] = Evaluation(i);
				
				System.out.format("%s Train/Test finished in %.2f seconds...\n", this.toString(), (System.currentTimeMillis()-start)/1000.0);
				m_trainSet.clear();
				m_testSet.clear();			
			}
			
		}
		double mean = Utils.sumOfArray(perf)/k, var = 0;
		for(int i=0; i<perf.length; i++)
			var += (perf[i]-mean) * (perf[i]-mean);
		var = Math.sqrt(var/k);
		System.out.format("Perplexity %.3f+/-%.3f\n", mean, var);
	}
	
	public double Evaluation(int i) {
		m_collectCorpusStats = false;
		double perplexity = 0, loglikelihood, totalWords=0, sumLikelihood = 0;
		
		System.out.println("In Normal");
		
		for(_Doc d:m_testSet) {				
			loglikelihood = inference(d);
			sumLikelihood += loglikelihood;
			perplexity += loglikelihood;
			totalWords += d.getTotalDocLength();
			for(_ChildDoc cDoc: ((_ParentDoc)d).m_childDocs){
				totalWords += cDoc.getTotalDocLength();
			}
		}
		System.out.println("total Words\t"+totalWords+"perplexity\t"+perplexity);
		perplexity /= totalWords;
		perplexity = Math.exp(-perplexity);
		sumLikelihood /= m_testSet.size();

		System.out.format("Test set perplexity is %.3f and log-likelihood is %.3f\n", perplexity, sumLikelihood);
		
		return perplexity;		
	}
	
	@Override
	public double inference(_Doc pDoc){
		ArrayList<_Doc> sampleTestSet = new ArrayList<_Doc>();
		
		initTest(sampleTestSet, pDoc);
	
		double logLikelihood = 0.0, count = 0;
		int  iter = 0;
		do {
			int t;
			_Doc tmpDoc;
			for(int i=sampleTestSet.size()-1; i>1; i--) {
				t = m_rand.nextInt(i);
				
				tmpDoc = sampleTestSet.get(i);
				sampleTestSet.set(i, sampleTestSet.get(t));
				sampleTestSet.set(t, tmpDoc);			
			}
			
			for(_Doc doc: sampleTestSet)
				calculate_E_step(doc);
			
			if (iter>m_burnIn && iter%m_lag==0){
				double tempLogLikelihood = 0;
				for(_Doc doc: sampleTestSet){
					
					collectStats( doc);
						// tempLogLikelihood += calculate_log_likelihood((_ParentDoc) doc);
				
					
				}
				count ++;
				// if(logLikelihood == 0)
				// 	logLikelihood = tempLogLikelihood;
				// else{

				// 	logLikelihood = Utils.logSum(logLikelihood, tempLogLikelihood);
				// }
			}
		} while (++iter<this.number_of_iteration);

		for(_Doc doc: sampleTestSet){
			estThetaInDoc(doc);
			logLikelihood += calculate_log_likelihood(doc);
		}
	

		return logLikelihood; 	
	}

	protected void initTest(ArrayList<_Doc> sampleTestSet, _Doc d){
		_ParentDoc pDoc = (_ParentDoc)d;
		for(_Stn stnObj: pDoc.getSentences()){
			stnObj.setTopicsVct(number_of_topics);
		}
		pDoc.setTopics4Gibbs(number_of_topics, d_alpha);		
		sampleTestSet.add(pDoc);
		
		for(_ChildDoc cDoc: pDoc.m_childDocs){
			cDoc.setTopics4Gibbs_LDA(number_of_topics, d_alpha);
			sampleTestSet.add(cDoc);
		}
	}
	
	public double logLikelihoodByIntegrateTopics(_Doc d){
		double docLogLikelihood = 0.0;
		_SparseFeature[] fv = d.getSparse();
		
		for(int j=0; j<fv.length; j++){
			int wid = fv[j].getIndex();
			double value = fv[j].getValue();
			
			double wordLogLikelihood = 0;
			for(int k=0; k<number_of_topics; k++){
//				if(topic_term_probabilty[k][index] == 0){
//					topic_term_probabilty[k][index] = 1e-9;
//				}
				double wordPerTopicLikelihood = (word_topic_sstat[k][wid]/m_sstat[k])*d.m_topics[k]; 
				wordLogLikelihood += wordPerTopicLikelihood;
				// double wordPerTopicLikelihood = Math.log(topic_term_probabilty[k][index]);
//				System.out.println("first part\t"+wordPerTopicLikelihood);
//				if(d.m_topics[k] == 0){
//					d.m_topics[k] = 1e-9;
//				}
				// wordPerTopicLikelihood += Math.log(d.m_topics[k]);
//				System.out.println("second part\t"+wordPerTopicLikelihood);
				// if(wordLogLikelihood == 0){
				// 	wordLogLikelihood = wordPerTopicLikelihood;
				// }else{
				// 	wordLogLikelihood = Utils.logSum(wordLogLikelihood, wordPerTopicLikelihood);
				// }
			}
			if(wordLogLikelihood < 1e-10){
				wordLogLikelihood += 1e-10;
				System.out.println("small log likelihood per word");
			}

			wordLogLikelihood = Math.log(wordLogLikelihood);

			docLogLikelihood += value*wordLogLikelihood;
		}

		return docLogLikelihood;
	}

	public double calculate_log_likelihood(_Doc d){
		if (d instanceof _ParentDoc)
			return logLikelihoodByIntegrateTopics((_ParentDoc)d);
		else
			return logLikelihoodByIntegrateTopics((_ChildDoc)d);
	}
	
	@Override
 	public void printTopWords(int k, String betaFile) {
		Arrays.fill(m_sstat, 0);

		System.out.println("print top words");
		for (_Doc d : m_trainSet) {
			for (int i = 0; i < number_of_topics; i++)
				m_sstat[i] += m_logSpace ? Math.exp(d.m_topics[i])
						: d.m_topics[i];	
		}

		Utils.L1Normalization(m_sstat);

		try {
			System.out.println("beta file");
			PrintWriter betaOut = new PrintWriter(new File(betaFile));
			for (int i = 0; i < topic_term_probabilty.length; i++) {
				MyPriorityQueue<_RankItem> fVector = new MyPriorityQueue<_RankItem>(
						k);
				for (int j = 0; j < vocabulary_size; j++)
					fVector.add(new _RankItem(m_corpus.getFeature(j),
							topic_term_probabilty[i][j]));

				betaOut.format("Topic %d(%.3f):\t", i, m_sstat[i]);
				for (_RankItem it : fVector) {
					betaOut.format("%s(%.3f)\t", it.m_name,
							m_logSpace ? Math.exp(it.m_value) : it.m_value);
					System.out.format("%s(%.3f)\t", it.m_name,
						m_logSpace ? Math.exp(it.m_value) : it.m_value);
				}
				betaOut.println();
				System.out.println();
			}
	
			betaOut.flush();
			betaOut.close();
		} catch (Exception ex) {
			System.err.print("File Not Found");
		}

		double loglikelihood = calLogLikelihoodByIntegrateTopics(0);
		System.out.format("Final Log Likelihood %.3f\t", loglikelihood);
		
		String filePrefix = betaFile.replace("topWords.txt", "");
		debugOutput(filePrefix);
		
	}
	
	public void debugOutput(String filePrefix){

		File topicFolder = new File(filePrefix + "topicAssignment");
	
		if (!topicFolder.exists()) {
			System.out.println("creating directory" + topicFolder);
			topicFolder.mkdir();
		}

		File childTopKStnFolder = new File(filePrefix+"topKStn");
		if(!childTopKStnFolder.exists()){
			System.out.println("creating top K stn directory\t"+childTopKStnFolder);
			childTopKStnFolder.mkdir();
		}
		
		File stnTopKChildFolder = new File(filePrefix+"topKChild");
		if(!stnTopKChildFolder.exists()){
			System.out.println("creating top K child directory\t"+stnTopKChildFolder);
			stnTopKChildFolder.mkdir();
		}
		
		int topKStn = 10;
		int topKChild = 10;
		for (_Doc d : m_trainSet) {
			printTopicAssignment(d, topicFolder);
			if(d instanceof _ParentDoc){
				printTopKChild4Stn(topKChild, (_ParentDoc)d, stnTopKChildFolder);
				printTopKStn4Child(topKStn, (_ParentDoc)d, childTopKStnFolder);
			}
		}

		String parentParameterFile = filePrefix + "parentParameter.txt";
		String childParameterFile = filePrefix + "childParameter.txt";
	
		printParameter(parentParameterFile, childParameterFile);
		
		String similarityFile = filePrefix+"topicSimilarity.txt";
		discoverSpecificComments(similarityFile);
		printEntropy(filePrefix);
	}

	void discoverSpecificComments(String similarityFile) {
		System.out.println("topic similarity");
	
		try {
			PrintWriter pw = new PrintWriter(new File(similarityFile));

			for (_Doc doc : m_trainSet) {
				if (doc instanceof _ParentDoc) {
					pw.print(doc.getName() + "\t");
					double stnTopicSimilarity = 0.0;
					double docTopicSimilarity = 0.0;
					for (_ChildDoc cDoc : ((_ParentDoc) doc).m_childDocs) {
						pw.print(cDoc.getName() + ":");

						docTopicSimilarity = computeSimilarity(((_ParentDoc) doc).m_topics, cDoc.m_topics);
						pw.print(docTopicSimilarity);
						for (_Stn stnObj:doc.getSentences()) {
						
							stnTopicSimilarity = computeSimilarity(stnObj.m_topics, cDoc.m_topics);
							
							pw.print(":"+(stnObj.getIndex()+1) + ":" + stnTopicSimilarity);
						}
						pw.print("\t");
					}
					pw.println();
				}
			}
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	

	double computeSimilarity(double[] topic1, double[] topic2) {
		return Utils.cosine(topic1, topic2);
	}
	
	
	public void printTopicAssignment(_Doc d, File topicFolder) {
	//	System.out.println("printing topic assignment parent documents");
		
		String topicAssignmentFile = d.getName() + ".txt";
		try {
			PrintWriter pw = new PrintWriter(new File(topicFolder,
					topicAssignmentFile));
			
			for(_Word w:d.getWords()){
				int index = w.getIndex();
				int topic = w.getTopic();
				String featureName = m_corpus.getFeature(index);
				pw.print(featureName + ":" + topic + "\t");
			}
			
			pw.flush();
			pw.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	
	public void printParameter(String parentParameterFile, String childParameterFile){
		System.out.println("printing parameter");
		try{
			System.out.println(parentParameterFile);
			System.out.println(childParameterFile);
			
			PrintWriter parentParaOut = new PrintWriter(new File(parentParameterFile));
			PrintWriter childParaOut = new PrintWriter(new File(childParameterFile));
			for(_Doc d: m_trainSet){
				if(d instanceof _ParentDoc){
					parentParaOut.print(d.getName()+"\t");
					parentParaOut.print("topicProportion\t");
					for(int k=0; k<number_of_topics; k++){
						parentParaOut.print(d.m_topics[k]+"\t");
					}
					
					for(_Stn stnObj:d.getSentences()){							
						parentParaOut.print("sentence"+(stnObj.getIndex()+1)+"\t");
						for(int k=0; k<number_of_topics;k++){
							parentParaOut.print(stnObj.m_topics[k]+"\t");
						}
					}
					
					parentParaOut.println();
					
				}else{
//					if(d instanceof _ChildDoc){
						childParaOut.print(d.getName()+"\t");

						childParaOut.print("topicProportion\t");
						for (int k = 0; k < number_of_topics; k++) {
							childParaOut.print(d.m_topics[k] + "\t");
						}
						
						
						childParaOut.println();
//					}
				}
			}
			
			parentParaOut.flush();
			parentParaOut.close();
			
			childParaOut.flush();
			childParaOut.close();
		}
		catch (Exception e) {
			e.printStackTrace();
//			e.printStackTrace();
//			System.err.print("para File Not Found");
		}

	}
	
	
	protected void printEntropy(String filePrefix){
		String entropyFile = filePrefix+"entropy.txt";
		boolean logScale = true;
		
		try{
			PrintWriter entropyPW = new PrintWriter(new File(entropyFile));
			
			for(_Doc d: m_trainSet){
				double entropyValue = 0.0;
				entropyValue = Utils.entropy(d.m_topics, logScale);
				entropyPW.print(d.getName()+"\t"+entropyValue);
				entropyPW.println();
			}
			entropyPW.flush();
			entropyPW.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		
		
		
	} 

	//p(w)= \sum_z p(w|z)p(z)
	protected double calLogLikelihoodByIntegrateTopics(int iter){
		double logLikelihood = 0.0;
		
		for(_Doc d: m_trainSet){
			logLikelihood += docLogLikelihoodByIntegrateTopics(d);
		}
		
		return logLikelihood;
	}
	
	
	protected double docLogLikelihoodByIntegrateTopics(_Doc d){
		
		double docLogLikelihood = 0.0;
		_SparseFeature[] fv = d.getSparse();
		
		for(int j=0; j<fv.length; j++){
			int index = fv[j].getIndex();
			double value = fv[j].getValue();
			
			double wordLogLikelihood = 0;
			for(int k=0; k<number_of_topics; k++){
				double wordPerTopicLikelihood = Math.log(topic_term_probabilty[k][index]);
				wordPerTopicLikelihood += Math.log(d.m_topics[k]);
				if(wordLogLikelihood == 0){
					wordLogLikelihood = wordPerTopicLikelihood;
				}else{
					wordLogLikelihood = Utils.logSum(wordLogLikelihood, wordPerTopicLikelihood);
				}
			}
			docLogLikelihood += value*wordLogLikelihood;
		}
		
		return docLogLikelihood;
	}
	
	
	//comment is a query, retrieve stn by topical similarity
	protected HashMap<Integer, Double> rankStn4ChildBySim( _ParentDoc pDoc, _ChildDoc cDoc){

		HashMap<Integer, Double> stnSimMap = new HashMap<Integer, Double>();
		
		for(_Stn stnObj:pDoc.getSentences()){
			double stnSim = computeSimilarity(cDoc.m_topics, stnObj.m_topics);
			stnSimMap.put(stnObj.getIndex()+1, stnSim);
		}
		
		return stnSimMap;
	}
	
	//stn is a query, retrieve comment by likelihood
	protected HashMap<String, Double> rankChild4StnByLikelihood(_Stn stnObj, _ParentDoc pDoc){
	
			HashMap<String, Double>childLikelihoodMap = new HashMap<String, Double>();

			for(_ChildDoc cDoc:pDoc.m_childDocs){
				int cDocLen = cDoc.getTotalDocLength();
				
				double stnLogLikelihood = 0;
				for(_Word w: stnObj.getWords()){
					double wordLikelihood = 0;
					int wid = w.getIndex();
				
					for(int k=0; k<number_of_topics; k++){
						wordLikelihood += (word_topic_sstat[k][wid]/m_sstat[k])*((cDoc.m_sstat[k]+d_alpha)/(d_alpha*number_of_topics+cDocLen));
					}
					
					stnLogLikelihood += Math.log(wordLikelihood);
				}
				childLikelihoodMap.put(cDoc.getName(), stnLogLikelihood);
			}
			
			return childLikelihoodMap;
//			if(cDoc.m_stnLikelihoodMap.containsKey(stnObj.getIndex()))
//				stnLogLikelihood += cDoc.m_stnLikelihoodMap.get(stnObj.getIndex());
//			cDoc.m_stnLikelihoodMap.put(stnObj.getIndex(), stnLogLikelihood);
//		}	
	}
	
	protected List<Map.Entry<Integer, Double>> sortHashMap4Integer(HashMap<Integer, Double> stnLikelihoodMap, boolean descendOrder){
		List<Map.Entry<Integer, Double>> sortList = new ArrayList<Map.Entry<Integer, Double>>(stnLikelihoodMap.entrySet());
		
		if(descendOrder == true){
			Collections.sort(sortList, new Comparator<Map.Entry<Integer, Double>>() {
				public int compare(Entry<Integer, Double> e1, Entry<Integer, Double> e2){
					return e2.getValue().compareTo(e1.getValue());
				}
			});
		}else{
			Collections.sort(sortList, new Comparator<Map.Entry<Integer, Double>>() {
				public int compare(Entry<Integer, Double> e1, Entry<Integer, Double> e2){
					return e2.getValue().compareTo(e1.getValue());
				}
			});
		}
		
		return sortList;

	}
	
	protected List<Map.Entry<String, Double>> sortHashMap4String(HashMap<String, Double> stnLikelihoodMap, boolean descendOrder){
		List<Map.Entry<String, Double>> sortList = new ArrayList<Map.Entry<String, Double>>(stnLikelihoodMap.entrySet());
		
		if(descendOrder == true){
			Collections.sort(sortList, new Comparator<Map.Entry<String, Double>>() {
				public int compare(Entry<String, Double> e1, Entry<String, Double> e2){
					return e2.getValue().compareTo(e1.getValue());
				}
			});
		}else{
			Collections.sort(sortList, new Comparator<Map.Entry<String, Double>>() {
				public int compare(Entry<String, Double> e1, Entry<String, Double> e2){
					return e2.getValue().compareTo(e1.getValue());
				}
			});
		}
		
		return sortList;

	}

	protected void printTopKChild4Stn(int topK, _ParentDoc pDoc, File topKChildFolder){
		File topKChild4PDocFolder = new File(topKChildFolder, pDoc.getName());
		if(!topKChild4PDocFolder.exists()){
//			System.out.println("creating top K stn directory\t"+topKChild4PDocFolder);
			topKChild4PDocFolder.mkdir();
		}
		
		for(_Stn stnObj:pDoc.getSentences()){
			HashMap<String, Double> likelihoodMap = rankChild4StnByLikelihood(stnObj, pDoc);
			String topChild4StnFile =  (stnObj.getIndex()+1)+".txt";
				
			try{
				int i=0;
				
				PrintWriter pw = new PrintWriter(new File(topKChild4PDocFolder, topChild4StnFile));
				
				for(Map.Entry<String, Double> e: sortHashMap4String(likelihoodMap, true)){
					if(i==topK)
						break;
					pw.print(e.getKey());
					pw.print("\t"+e.getValue());
					pw.println();
					
					i++;
				}
				
				pw.flush();
				pw.close();
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	protected void printTopKStn4Child(int topK, _ParentDoc pDoc, File topKStnFolder){
		File topKStn4PDocFolder = new File(topKStnFolder, pDoc.getName());
		if(!topKStn4PDocFolder.exists()){
//			System.out.println("creating top K stn directory\t"+topKStn4PDocFolder);
			topKStn4PDocFolder.mkdir();
		}
		
		for(_ChildDoc cDoc:pDoc.m_childDocs){
			String topKStn4ChildFile = cDoc.getName()+".txt";
			HashMap<Integer, Double> stnSimMap = rankStn4ChildBySim(pDoc, cDoc);

			try{
				int i=0;
				
				PrintWriter pw = new PrintWriter(new File(topKStn4PDocFolder, topKStn4ChildFile));
				
				for(Map.Entry<Integer, Double> e: sortHashMap4Integer(stnSimMap, true)){
					if(i==topK)
						break;
					pw.print(e.getKey());
					pw.print("\t"+e.getValue());
					pw.println();
					
					i++;
				}
				
				pw.flush();
				pw.close();
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
}
