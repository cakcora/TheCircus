See BitcoinHeist: Topological Data Analysis for Ransomware Detection on the Bitcoin Blockchain at https://arxiv.org/pdf/1906.07852.pdf for details.

First a description of our ransomware data:


allAddresses.txt contains ransomware (RS) addresses from a union of Princeton, Padua and Montreal datasets. The paper has references to them. Each line in this file lists an address, and its RS label. If an address appears in multiple datasets, each of its labels are given in the second column. Example lines are given below:


1BFguNQ3CWURg8TTqkiowF9dWbh4dgcqo4	paduaCryptoLocker	montrealCryptoLocker
1DMBVtH3yL8u2vd9BPeB7L3cssjZpAuJsw	princetonLocky	montrealLocky

In the first line, the address that ends with ...4dgcqo4 has been identified as belonging to the CryptoLocker RS by both montreal and padua studies. Note that in some addresses, studies conflict about the label.



Second, a description of our feature data. We follow these steps:

1- We extract edges from the Bitcoin blockchain.
2- From edges, we create a network and filter out edges that transfer less than 0.3 bitcoins. 
3- We divide the network into 24 hour time windows.
4- In each time window, we compute six features of addresses. 

Below, you will find a description of these features.

RS Address Features:

An RS address can appear in multiple time windows. For each appearance we compute its features in the graph of that given day. Features of RS addresses are stored in the ransom.csv.
Lines are as given by column names:
2011,246,199aKfZUiNXFuk3TADJB2JpWVNXKReL3yw,2,1.5,2,0,1,7e+07
2011,254,199aKfZUiNXFuk3TADJB2JpWVNXKReL3yw,60,4.89385504471628e-07,39,0,2,3.07e+08


For feature descriptions, please see the paper. Income is given in terms of Satoshis (1Bitcoin=10^8 Satoshis).

White Address Features:


The files are bigger than 100MB, could not be uploaded to Github. We share them on dropbox:

white1K: https://www.dropbox.com/s/d4uu7c4m079j4y2/white1K.csv?dl=0
white2K: https://www.dropbox.com/s/8pb8oe9iuav6u56/white2K.csv?dl=0

Next, we look at addresses that are not yet known to be RS addresses; we term them white addresses. In a day there can be >300K white addresses. We sample 1K, 2K, 10K and 100K addresses for each day. If the day has less than what we use, we take all addresses.  These are given in white1K and white2K.csv. White100K is 4.3GB, not included. If you need it, please email Cuneyt Akcora.

Example lines are: 
year,day,address,length,weight,count,looped,neighbors,income,virus
2011,1,19snqSYnDSC4mDbv3pJuYgYqm5ctqwAxnm,0,1,1,0,1,5e+09,white
2011,1,1FeGY25MSKtnwVNGrnbwLsecBpBYrMQ6Kx,0,1,1,0,2,5e+09,white


The virus column is always white in this white files. I kept this redundant information for defensive coding.
White1K is exactly what we use in the paper.  
