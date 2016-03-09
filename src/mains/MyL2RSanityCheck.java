package mains;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import SanityCheck.AnnotatedSanityCheck;
import SanityCheck.BaseSanityCheck;
import opennlp.tools.util.InvalidFormatException;
import structures._Corpus;
import Analyzer.Analyzer;
import Analyzer.AspectAnalyzer;
import Analyzer.DocAnalyzer;

/****
 * In this main function, I will apply learning to rank models on the human annotated different groups of reviews 
 * to see if learning to rank can benefit from the grouping.  
 * @author lin
 */
public class MyL2RSanityCheck {
	public static void main(String[] args) throws InvalidFormatException, FileNotFoundException, IOException{
		int classNumber = 5;
		int Ngram = 2; //The default value is unigram. 
		int lengthThreshold = 5; //Document length threshold
		int minimunNumberofSentence = 2; // each sentence should have at least 2 sentences for HTSM, LRSHTM
		int number_of_topics = 30;
		
		//Added by Mustafizur----------------
		String pathToPosWords = "./data/Model/SentiWordsPos.txt";
		String pathToNegWords = "./data/Model/SentiWordsNeg.txt";
		String pathToNegationWords = "./data/Model/negation_words.txt";
		String pathToSentiWordNet = "./data/Model/SentiWordNet_3.0.0_20130122.txt";
		
		/*****The parameters used in loading files.*****/
		String folder = "./data/amazon/small/dedup/RawData";
		String suffix = ".json";
		String tokenModel = "./data/Model/en-token.bin"; //Token model.

		String stnModel = "./data/Model/en-sent.bin"; //Sentence model. Need it for postagging.
		String tagModel = "./data/Model/en-pos-maxent.bin";
		
		String category = "tablets"; //"electronics"
		String dataSize = "86jsons"; //"50K", "100K"
		String fvFile = String.format("./data/Features/fv_%dgram_%s_%s.txt", Ngram, category, dataSize);
		String fvStatFile = String.format("./data/Features/fv_%dgram_stat_%s_%s.txt", Ngram, category, dataSize);
//		String stopwords = "./data/Model/stopwords.dat";
		
		int numOfAspects = 28; // 12, 14, 24, 28
		String aspectlist = String.format("./data/Model/%d_aspect_tablet.txt", numOfAspects);
		String topicFile = String.format("./data/TopicVectors/%dAspects_topicVectors_corpus.txt", numOfAspects);
		
		System.out.println("Creating feature vectors, wait...");
		Analyzer analyzer;
		_Corpus c;
		analyzer = new AspectAnalyzer(tokenModel, stnModel, classNumber, fvFile, Ngram, lengthThreshold, tagModel, aspectlist, true);		
		((DocAnalyzer) analyzer).setReleaseContent(false);
		analyzer.setMinimumNumberOfSentences(minimunNumberofSentence);
		((DocAnalyzer) analyzer).loadPriorPosNegWords(pathToSentiWordNet, pathToPosWords, pathToNegWords, pathToNegationWords);
		analyzer.LoadDirectory(folder, suffix); //Load all the documents as the data set.
		analyzer.loadTopicVectors(topicFile, number_of_topics);

		String stnLabel = "./data/StnLabels.txt";
		analyzer.documentsMatch(analyzer.loadStnLabels(stnLabel), number_of_topics/2);
		
		//construct effective feature values for supervised classifiers 
		analyzer.setFeatureValues("BM25", 2);
		c = analyzer.returnCorpus(fvStatFile); // Get the collection of all the documents.
		c.mapLabels(4);
	
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMdd-HHmm");	
		String compareFile = String.format("./data/%s_Compare.output", dateFormatter.format(new Date()));

		AnnotatedSanityCheck check = new AnnotatedSanityCheck(c, "SVM", 1, 100, BaseSanityCheck.SimType.ST_L2R);
		check.loadAnnotatedFile("./data/Selected100Files/100Files_IDs_Annotation.txt");
//		check.calculatePrecision(0.2);
//		check.compareAnnotation(compareFile);
		
		String weightFile = String.format("./data/%s_SanityCheck_weight.output", dateFormatter.format(new Date()));
		check.initWriter(weightFile);
		check.diffGroupLOOCV();
	}
}