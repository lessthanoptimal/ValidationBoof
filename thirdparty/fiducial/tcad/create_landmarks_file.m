addpath('./TCAD_release/MyDetector/');
addpath('./TCAD_release/Pattern/');

load('PatternInfo.mat');

rows = PatternMatrixSize(1);
cols = PatternMatrixSize(2);

fid = fopen(sprintf("corners_%dx%d.txt",rows,cols),'w');
fprintf(fid,"# TCAD marker: rows=%d cols=%d\n",rows,cols);
fprintf(fid,"paper=LETTER\n");
fprintf(fid,"units=INCH\n");
fprintf(fid,"count=%d\n",size(PatternPts,1));

for i = 1 : size(PatternPts,1)
    col = floor((i-1)/rows);
    row = mod((i-1),rows);
    id = row*cols + col;
    x = (PatternPts(i,1)-1.0)/100.0;
    y = (1100-PatternPts(i,2)+1.0)/100.0;
    fprintf(fid,"%d %.6f %.6f\n", id, x, y);
end

fclose(fid);