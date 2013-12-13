from matplotlib.backends.backend_pdf import PdfPages
from pylab import *

# Storage for descriptor results from one file
class DescribeInfo:

    def __init__(self):
        self.listName = []
        self.listCounts = []
        self.listRecall = []
        self.listPrecision = []
        self.listFMeas = []

    def setSummary(self,numFeatures,maxMatches,sumPrecision,sumRecall,sumFMeasure):
        self.numFeatures = numFeatures
        self.maxMatches = maxMatches
        self.sumPrecision = sumPrecision
        self.sumRecall = sumRecall
        self.sumFMeasure = sumFMeasure

    def addSet(self,name,counts,precision,recall,fmeas):
        self.listName.append(name)
        self.listCounts.append(counts)
        self.listRecall.append(precision)
        self.listPrecision.append(recall)
        self.listFMeas.append(fmeas)

# Parses descriptor results from the specified file
def parseDescribe(fileName):

    info = DescribeInfo()

    f = open(fileName,'r')

    # Skip header
    f.readline()
    f.readline()
    f.readline()

    while True:
        firstLine = f.readline()
        if firstLine.find('Summary') != -1:
            # Parse Summary Data
            numFeatures = int(f.readline().strip().split(' ')[-1])
            maxMatches = int(f.readline().strip().split(' ')[-1])
            sumPrecision = float(f.readline().strip().split(' ')[-1])
            sumRecall = float(f.readline().strip().split(' ')[-1])
            sumFMeasure = float(f.readline().strip().split(' ')[-1])

            info.setSummary(numFeatures,maxMatches,sumPrecision,sumRecall,sumFMeasure)
            break
        else:
            # read in which data set was processed
            N = len('---------- Directory: ')
            pass
            imageName = firstLine[N:-1]
            # Number of images in this set
            f.readline()
            # read in performance metrics
            counts = [int(n) for n in f.readline().strip().split(' ')]
            precision = [float(n) for n in f.readline().strip().split(' ')]
            recall = [float(n) for n in f.readline().strip().split(' ')]
            fmeasure = [float(n) for n in f.readline().strip().split(' ')]
            info.addSet(imageName,counts,precision,recall,fmeasure)

    return info


names = []
names.append(["BoofCV_MSURF","SURF-M",'r',"-"])
names.append(["BoofCV_SURF","SURF-F",'lightgreen',"-"])
names.append(["JavaSURF","JavaSURF",'lightblue',"-"])
names.append(["JOpenSURF","JOpenSURF",'k',"-"])
names.append(["OpenCV_SURF","OpenCV",'m',"--"])
names.append(["OpenSURF","OpenSURF",'c',"-."])
names.append(["PanOMatic","Pan-o-Matic",'b',"--"])
names.append(["SURF","Reference",'g',"-."])

info = []

for x in names:
    info.append(parseDescribe('../results/describe_stability_'+x[0]+'.txt'))

imageNames = [ x.split('/')[-2] for x in info[0].listName ]

# Create a summary plot
xnums = range(len(info))
Y = [x.sumFMeasure for x in info ]
labels = [x[1] for x in names ]
colors = [x[2] for x in names ]

f = figure()

pp = PdfPages('overall_descriptor_stability.pdf')

ax = f.add_axes([0.1, 0.2, 0.8, 0.7])

ax.bar(xnums, Y, align='center',color=colors)
ax.set_xticks(xnums)
ax.set_xticklabels(labels,None,False,rotation=30)
ax.get_xaxis().set_label_text("Library",fontsize=16,weight=1000)
ax.get_yaxis().set_label_text("Sum F-Statistic",fontsize=16,weight=1000)
ax.set_title("Overall Descriptor Stability",fontsize=24,weight=1000)


for x,y in zip(xnums,Y):
    ax.text(x, y+0.2, '%.2f' % y, ha='center', va= 'bottom')

pp.savefig(f)
pp.close()
f.show()

# Create plots for individual images

for imgNum in range(len(imageNames)):
    pp = PdfPages('describe_stability_'+imageNames[imgNum]+'.pdf')
    f = figure()
    ax = f.add_axes([0.1, 0.1, 0.8, 0.8])
    num_images = len(info[0].listFMeas)
    ax.set_xticks(range(num_images))
    ax.set_ylim([0,1])
    X = range(len(info[0].listFMeas[imgNum]))
    for i in range(len(info)):
        opt = names[i]
        pts=ax.plot(X, info[i].listFMeas[imgNum],linewidth=4,label=opt[1],color=opt[2],linestyle=opt[3])
    ax.legend()
    ax.get_xaxis().set_label_text("Image",fontsize=16,weight=1000)
    ax.get_yaxis().set_label_text("F-Statistic",fontsize=16,weight=1000)
    ax.set_title("Descriptor Stability in "+imageNames[imgNum],fontsize=24,weight=1000)
    pp.savefig(f)
    pp.close()
    f.show()

# pause
show()


