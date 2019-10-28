import torch
from IPython import display
from matplotlib import pyplot as plt
import numpy as np
import random
import d2lzh_pytorch 
num_inputs = 2
num_examples = 1000
true_w = [2, -3.4]
true_b = 4.2
features = torch.from_numpy(np.random.normal(0, 1, (num_examples, num_inputs)))
labels = true_w[0] * features[:, 0] + true_w[1] * features[:, 1] + true_b
labels += torch.from_numpy(np.random.normal(0, 0.01, size=labels.size()))
set_figsize()
plt.scatter(features[:, 1].numpy(), labels.numpy(), 1)
