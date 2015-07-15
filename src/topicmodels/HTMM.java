package topicmodels;

import java.util.Arrays;

import markovmodel.FastRestrictedHMM;
import structures._Corpus;
import structures._Doc;
import structures._SparseFeature;
import structures._Stn;
import utils.Utils;

public class HTMM extends pLSA {
	// HTMM parameter both in log space
	double epsilon;   // estimated epsilon
	
	// cache structure
	double[][] p_dwzpsi;  // The state probabilities that is Pr(z,psi | d,w)
 	double[][] emission;  // emission probability of p(s|z)
	
 	// HMM-style inferencer
 	FastRestrictedHMM m_hmm; 
 	
	// sufficient statistics for p(\epsilon)
	int total; 
	double lot;
	
	double loglik;
	int constant;

	public HTMM(int number_of_iteration, double converge, double beta, _Corpus c, //arguments for general topic model
			int number_of_topics, double alpha) {//arguments for pLSA	
		super(number_of_iteration, converge, beta, c,
				0, null, //HTMM does not have a background setting
				number_of_topics, alpha);
		
		this.constant = 2;		
		this.epsilon = Math.random();
		
		int maxSeqSize = c.getLargestSentenceSize();		
		m_hmm = new FastRestrictedHMM(epsilon, maxSeqSize, this.number_of_topics, this.constant); 
		
		//cache in order to avoid frequently allocating new space
		p_dwzpsi = new double[maxSeqSize][this.constant * this.number_of_topics]; // max|S_d| * (2*K)
		emission = new double[maxSeqSize][this.number_of_topics]; // max|S_d| * K
	}
	
	public HTMM(int number_of_iteration, double converge, double beta, _Corpus c, //arguments for general topic model
			int number_of_topics, double alpha, int constant) {//arguments for pLSA	
		super(number_of_iteration, converge, beta, c,
				0, null, //HTMM does not have a background setting
				number_of_topics, alpha);
		
		this.epsilon = Math.random();
		this.constant = constant;
		
		int maxSeqSize = c.getLargestSentenceSize();		
		//cache in order to avoid frequently allocating new space
		p_dwzpsi = new double[maxSeqSize][this.constant * this.number_of_topics]; // max|S_d| * (2*K)
		emission = new double[maxSeqSize][this.number_of_topics]; // max|S_d| * K
	}
	
	public HTMM(int number_of_iteration, double converge, double beta, _Corpus c, //arguments for general topic model
			int number_of_topics, double alpha, //arguments for pLSA
			boolean setHMM) { //just to indicate we don't need initiate hmm inferencer
		super(number_of_iteration, converge, beta, c,
				0, null, //HTMM does not have a background setting
				number_of_topics, alpha);
		
		this.epsilon = Math.random();
		this.constant = 2;
		
		int maxSeqSize = c.getLargestSentenceSize();		
		if (setHMM)
			m_hmm = new FastRestrictedHMM(epsilon, maxSeqSize, this.number_of_topics, this.constant); 
		else
			m_hmm = null;
		
		//cache in order to avoid frequently allocating new space
		p_dwzpsi = new double[maxSeqSize][constant * this.number_of_topics]; // max|S_d| * (2*K)
		emission = new double[maxSeqSize][this.number_of_topics]; // max|S_d| * K
	}
	
	@Override
	public String toString() {
		return String.format("HTMM[k:%d, alpha:%.3f, beta:%.3f]", number_of_topics, d_alpha, d_beta);
	}
	
	// Construct the emission probabilities for sentences under different topics in a particular document.
	void ComputeEmissionProbsForDoc(_Doc d) {
		for(int i=0; i<d.getSenetenceSize(); i++) {
			_Stn stn = d.getSentence(i);
			Arrays.fill(emission[i], 0);
			for(int k=0; k<this.number_of_topics; k++) {
				for(_SparseFeature w:stn.getFv()) {
					emission[i][k] += w.getValue() * topic_term_probabilty[k][w.getIndex()];//all in log-space
				}
			}
		}
	}
	
	@Override
	public double calculate_E_step(_Doc d) {
		//Step 1: pre-compute emission probability
		ComputeEmissionProbsForDoc(d);
		
		//Step 2: use forword/backword algorithm to compute the posterior
		double logLikelihood = m_hmm.ForwardBackward(d, emission) + docThetaLikelihood(d);
		loglik += logLikelihood;
		
		//Step 3: collection expectations from the posterior distribution
		m_hmm.collectExpectations(p_dwzpsi);//expectations will be in the original space	
		accTheta(d);
		
		if (m_collectCorpusStats) {
			accEpsilonStat(d);
			accPhiStat(d);
		}
		
		return logLikelihood;
	}
	
	public int[] get_MAP_topic_assignment(_Doc d) {
		int path [] = new int [d.getSenetenceSize()];
		m_hmm.BackTrackBestPath(d, emission, path);
		return path;
	}	

	//probabilities of topic switch
	void accEpsilonStat(_Doc d) {
		for(int t=1; t<d.getSenetenceSize(); t++) {
			for(int i=0; i<(this.constant-1)*this.number_of_topics; i++) 
				this.lot += this.p_dwzpsi[t][i];
			this.total ++;
		}
	}
	
	//probabilities of topic assignment
	void accPhiStat(_Doc d) {
		double prob;
		for(int t=0; t<d.getSenetenceSize(); t++) {
			_Stn s = d.getSentence(t);
			for(_SparseFeature f:s.getFv()) {
				int wid = f.getIndex();
				double v = f.getValue();//frequency
				for(int i=0; i<this.number_of_topics; i++) {
					prob = this.p_dwzpsi[t][i];
					for(int j=1; j<this.constant; j++)
						prob += this.p_dwzpsi[t][i + j*this.number_of_topics];
					this.word_topic_sstat[i][wid] += v * prob;
				}
			}
		}
	}
	
	void accTheta(_Doc d) {
		for(int t=0; t<d.getSenetenceSize(); t++) {
			for(int i=0; i<this.number_of_topics; i++) 
				for(int j=0; j<this.constant-1; j++)
					d.m_sstat[i] += this.p_dwzpsi[t][i + j*this.number_of_topics];//only consider \psi=1
		}
	}
	
	//accumulate sufficient statistics for theta, according to Eq(21) in HTMM note
	@Override
	protected void estThetaInDoc(_Doc d) {
		double sum = Math.log(Utils.sumOfArray(d.m_sstat));//prior has already been incorporated when initialize m_sstat
		for(int i=0; i<this.number_of_topics; i++) 
			d.m_topics[i] = Math.log(d.m_sstat[i]) - sum;//ensure in log-space
	}
	
	@Override
	public void calculate_M_step(int iter) {
		if (iter>0) {
			this.epsilon = this.lot/this.total; // to make the code structure concise and consistent, keep epsilon in real space!!
			m_hmm.setEpsilon(this.epsilon);
		}
		
		for(int i=0; i<this.number_of_topics; i++) {
			double sum = Math.log(Utils.sumOfArray(word_topic_sstat[i]));
			for(int v=0; v<this.vocabulary_size; v++)
				topic_term_probabilty[i][v] = Math.log(word_topic_sstat[i][v]) - sum;
		}
		
		for(_Doc d:m_trainSet)
			estThetaInDoc(d);
	}
	
	double docThetaLikelihood(_Doc d) {
		double logLikelihood = 0;
		for(int i=0; i<this.number_of_topics; i++)
			logLikelihood += (d_alpha-1)*d.m_topics[i];
		return logLikelihood;
	}
		
	protected void init() {
		super.init();
		
		this.loglik = 0;
		this.total = 0;
		this.lot = 0.0;// sufficient statistics for epsilon
	}
	
	@Override
	protected void initTestDoc(_Doc d) {
		super.initTestDoc(d);
		for(int i=0; i<d.m_topics.length; i++){//convert to log-space
			d.m_topics[i] = (double)1.0/this.number_of_topics;
			d.m_topics[i] = Math.log(d.m_topics[i]);
		}
	}
	
	//for HTMM, this function will be only called in testing phase to avoid duplicated computation
	@Override
	public double calculate_log_likelihood(_Doc d) {//it is very expensive to re-compute this
		//Step 1: pre-compute emission probability
		ComputeEmissionProbsForDoc(d);		
		
		double logLikelihood = 0;
		for(int i=0; i<this.number_of_topics; i++) 
			logLikelihood += (d_alpha-1)*d.m_topics[i];
		return logLikelihood + m_hmm.ForwardBackward(d, emission);
	}
	
	@Override
	public double inference(_Doc d) {
		double current = super.inference(d);
		
		int path[] = get_MAP_topic_assignment(d);
		_Stn[] sentences = d.getSentences();
		for(int i=0; i<path.length;i++)
			sentences[i].setTopic(path[i]);
		
		return current;
	}
	
}
