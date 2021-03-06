package Classifier.supervised.modelAdaptation.CoLinAdapt;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

import Classifier.supervised.modelAdaptation._AdaptStruct;
import Classifier.supervised.modelAdaptation.RegLR.asyncRegLR;
import structures._PerformanceStat.TestMode;
import structures._Review;
import structures._Review.rType;
import structures._UserReviewPair;

public class asyncMTLinAdapt extends MTLinAdapt {

	double m_initStepSize = 0.25;
	boolean m_trainByUser = false; // by default we will perform online training by user; otherwise we will do it by review timestamp 
	
	public asyncMTLinAdapt(int classNo, int featureSize,
			HashMap<String, Integer> featureMap, int topK, String globalModel,
			String featureGroupMap, String featureGroup4Sup) {
		super(classNo, featureSize, featureMap, topK, globalModel, featureGroupMap, featureGroup4Sup);
		
		// the default test mode for asyncMTLinAdapt is online
		m_testmode = TestMode.TM_online;
		
		// this mode is for validation purpose
//		m_testmode = TestMode.TM_batch;
	}
	
	@Override
	public String toString() {
		return String.format("asyncMTLinAdapt[dim:%d,SupDim:%d, eta1:%.3f,eta2:%.3f, lambda1:%.3f,lambda2:%.3f]", m_dim, m_dimSup, m_eta1, m_eta2, m_lambda1, m_lambda2);
	}
	
	@Override
	protected void calculateGradients(_AdaptStruct u){
		gradientByFunc(u);
		gradientByR1(u);
		gradientByRs();
	}
	
	@Override
	protected void init(){
		super.init();
		
		// this is also incorrect, since we should normalized it by total number of adaptation reviews
		m_lambda1 /= m_userSize;
		m_lambda2 /= m_userSize;
	}
	
	//this is online training in each individual user
	@Override
	public double train(){
		initLBFGS();
		init();
		
		if (m_trainByUser)
			trainByUser();
		else
			trainByReview();
		
		setPersonalizedModel();
		return 0;//we do not evaluate function value
	}	
	
	void trainByReview() {
		LinkedList<_UserReviewPair> reviewlist = new LinkedList<_UserReviewPair>();
		
		double gNorm, gNormOld = Double.MAX_VALUE;
		int predL, trueL, counter = 0;
		_Review doc;
		_CoLinAdaptStruct user;
		
		//collect the training/adaptation data
		for(int i=0; i<m_userList.size(); i++) {
			user = (_CoLinAdaptStruct)m_userList.get(i);
			for(_Review r:user.getReviews()) {
				if (r.getType() == rType.ADAPTATION || r.getType() == rType.TRAIN)
					reviewlist.add(new _UserReviewPair(user, r));//we will only collect the training or adaptation reviews
			}
		}
		
		//sort them by timestamp
		Collections.sort(reviewlist);
		
		for(_UserReviewPair pair:reviewlist) {
			user = (_CoLinAdaptStruct)pair.getUser();
			// test the latest model before model adaptation
			if (m_testmode != TestMode.TM_batch) {
				doc = pair.getReview();
				predL = predict(doc, user);
				trueL = doc.getYLabel();
				user.getPerfStat().addOnePredResult(predL, trueL);
			}// in batch mode we will not accumulate the performance during adaptation	
			
			gradientDescent(user, m_initStepSize, 1.0);
			
			//test the gradient only when we want to debug
			if (m_displayLv>0) {
				gNorm = gradientTest();				
				if (m_displayLv==1) {
					if (gNorm<gNormOld)
						System.out.print("o");
					else
						System.out.print("x");
				}				
				gNormOld = gNorm;
				if (++counter%120==0)
					System.out.println();
			}
		}
	}
	
	void trainByUser() {
		double gNorm, gNormOld = Double.MAX_VALUE;
		int predL, trueL;
		_Review doc;
		_CoLinAdaptStruct user;
		
		for(int i=0; i<m_userList.size(); i++) {
			user = (_CoLinAdaptStruct)m_userList.get(i);
			
			while(user.hasNextAdaptationIns()) {
				// test the latest model before model adaptation
				if (m_testmode != TestMode.TM_batch && (doc = user.getLatestTestIns()) != null) {
					predL = predict(doc, user);
					trueL = doc.getYLabel();
					user.getPerfStat().addOnePredResult(predL, trueL);
				} // in batch mode we will not accumulate the performance during adaptation				
				
				gradientDescent(user, m_initStepSize, 1.0);
				
				//test the gradient only when we want to debug
				if (m_displayLv>0) {
					gNorm = gradientTest();				
					if (m_displayLv==1) {
						if (gNorm<gNormOld)
							System.out.print("o");
						else
							System.out.print("x");
					}				
					gNormOld = gNorm;
				}
			}
			
			if (m_displayLv==1)
				System.out.println();
		}
	}
	
	@Override
	protected int getAdaptationSize(_AdaptStruct user) {
		return user.getAdaptationCacheSize();
	}
	
	@Override
	protected void gradientByFunc(_AdaptStruct user) {		
		//Update gradients one review by one review.
		for(_Review review:user.nextAdaptationIns())
			gradientByFunc(user, review, 1.0);//equal weight for the user's own adaptation data
	}
	
	// update this current user only
	void gradientDescent(_CoLinAdaptStruct user, double initStepSize, double inc) {
		double a, b, stepSize = asyncRegLR.getStepSize(initStepSize, user);
		int offset = 2 * m_dim * user.getId(), supOffset = 2 * m_dim * m_userList.size();
		
		//get gradient
		Arrays.fill(m_g, 0);
		calculateGradients(user);
		
		//update the individual user
		for (int k = 0; k < m_dim; k++) {
			a = user.getScaling(k) - stepSize * m_g[offset + k];
			user.setScaling(k, a);

			b = user.getShifting(k) - stepSize * m_g[offset + k + m_dim];
			user.setShifting(k, b);
		}
		
		//update the super user
		stepSize /= 3;
		for(int k=0; k<m_dimSup; k++) {
			m_A[supOffset+k] -= stepSize * m_g[supOffset + k];
			m_A[supOffset+k+m_dimSup] -= stepSize * m_g[supOffset + k + m_dimSup];
		}
		
		//update the record of updating history
		user.incUpdatedCount(inc);
	}
}
