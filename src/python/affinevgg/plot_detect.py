from matplotlib.backends.backend_pdf import PdfPages
from pylab import *

# Storage for detector results from one file
class DetectInfo:

    def __init__(self):
        self.listName = []
        self.listCounts = []
        self.listCorrect = []

    def setSummary(self,numMatches,totalCorrect,totalAmbiguous):
        self.numMatches = numMatches
        self.totalCorrect = totalCorrect
        self.totalAmbiguous = totalAmbiguous

    def addSet(self,name,counts,correct):
        self.listName.append(name)
        self.listCounts.append(counts)
        self.listCorrect.append(correct)

# Parses descriptor results from the specified file
def parseDetect(fileName):

    info = DetectInfo()

    f = open(fileName,'r')

    # Skip header
    f.readline()
    f.readline()
    f.readline()

    while True:
        firstLine = f.readline()
        if firstLine.find('Summary') != -1:
            # Parse Summary Data
            numMatches = int(f.readline().strip().split(' ')[-1])
            totalCorrect = float(f.readline().strip().split(' ')[-1])
            totalAmbiguous = float(f.readline().strip().split(' ')[-1])

            info.setSummary(numMatches,totalCorrect,totalAmbiguous)
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
            correct = [float(n) for n in f.readline().strip().split(' ')]
            info.addSet(imageName,counts,correct)

    return info

names = []
names.append(["FH","Proposed",'r',"-"])
names.append(["JavaSURF","JavaSURF",'lightblue',"-"])
names.append(["JOpenSURF","JOpenSURF",'k',"-"])
names.append(["OpenCV","OpenCV",'m',"--"])
names.append(["OpenSURF","OpenSURF",'c',"-."])
names.append(["PanOMatic","Pan-o-Matic",'b',"--"])
names.append(["SURF","Reference",'g',"-."])

info = []

for x in names:
    info.append(parseDetect('../results/detect_stability_'+x[0]+'.txt'))

sequenceNames = [ x.split('/')[-2] for x in info[0].listName ]

# Create a summary plot
xnums = range(len(info))
Y = [x.totalCorrect for x in info ]
labels = [x[1] for x in names ]
colors = [x[2] for x in names ]

f = figure()

pp = PdfPages('overall_detector_stability.pdf')

ax = f.add_axes([0.1, 0.2, 0.8, 0.7])
ax.bar(xnums, Y, align='center',color=colors)
ax.set_xticks(xnums)
ax.set_xticklabels(labels,None,False,rotation=30)
ax.get_xaxis().set_label_text("Library",fontsize=16,weight=1000)
ax.get_yaxis().set_label_text("Repeatability",fontsize=16,weight=1000)
ax.set_title("Overall Detector Stability",fontsize=24,weight=1000)


for x,y in zip(xnums,Y):
    ax.text(x, y+0.2, '%.2f' % y, ha='center', va= 'bottom')

pp.savefig(f)
pp.close()
f.show()

# Create plots for individual images

for imgNum in range(len(sequenceNames)):
    pp = PdfPages('detect_stability_'+sequenceNames[imgNum]+'.pdf')
    f = figure()
    ax = f.add_axes([0.1, 0.1, 0.8, 0.8])
    num_images = len(info[0].listCorrect)
    ax.set_xticks(range(num_images))
    ax.set_ylim([0,1])
    X = range(len(info[0].listCorrect[imgNum]))
    for i in range(len(info)):
        opt = names[i]
        pts=ax.plot(X, info[i].listCorrect[imgNum],linewidth=4,label=opt[1],color=opt[2],linestyle=opt[3])
    ax.legend()
    ax.get_xaxis().set_label_text("Image",fontsize=16,weight=1000)
    ax.get_yaxis().set_label_text("Repeatability",fontsize=16,weight=1000)
    ax.set_title("Detector Stability in "+sequenceNames[imgNum],fontsize=24,weight=1000)
    pp.savefig(f)
    pp.close()
    f.show()

# pause
show()