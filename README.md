# LaPLACE: Probabilistic Local Model-Agnostic Causal Explanations

The LaPLACE-Explainer component leverages the concept of a Markov blanket to establish statistical boundaries between relevant and non-relevant features. 
This approach results in the automatic generation of optimal feature subsets for predictions. 
https://arxiv.org/pdf/2310.00570.pdf

# Main Contributions
 ```
Do not need to predetermine a fixed number (N ) of top features as explanations.
Offers probabilistic cause-and-effect explanations.
Outperforms LIME and SHAP (well-known model-agnostic explainers) in terms of local accuracy and consistency of explained features.
```
We demonstrate the practical utility of these explanations via experiments with both simulated and real-world datasets. 
This encompasses addressing trust-related issues, such as evaluating prediction reliability, facilitating model selection, enhancing trustworthiness, 
and identifying fairness-related concerns within classifiers.

# Setting the environment
 ```
Install Java 1.8
Install Eclipse.
Download this repository.
Import this repository: "File"->"Import"->"Existing Projects into Workspace"; select this file path by pressing "Browse" in "Select root directory".
 ```
# Run the experiment
 ```
Download the datasets for the experiments in the paper at the following link: 
Go to src/main/java/exp.mb->LaPLACE_Explainer.java->void and Set file paths to dataFile (for data) and exp_File (for explanation).
 ```
https://drive.google.com/drive/folders/1uiC4DNhCZmbWDrBFIRqrTTggIeUSBdcT?usp=sharing
# Evaluate
 ```
Go to src/main/java/exp.mb->Evaluate.java->void and Set file paths to dataFile (for data) and exp_File (for explanation).
 ```
It will provide local accuracy of Random Forests; Bayesian network and Support Vector Machine on explained features by LaPLACE by validating with 20% of the dataset as test data.
# If you find this code useful, Please cite the following paper
 ```
@article{minn2023laplace,
  title={LaPLACE: Probabilistic Local Model-Agnostic Causal Explanations},
  author={Minn, Sein},
  journal={arXiv preprint arXiv:2310.00570},
  year={2023}
}
```
