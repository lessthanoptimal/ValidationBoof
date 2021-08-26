
% matlab -nodisplay -r "detect(\"input/path\",\"output/path\"),quit"
function exitcode=detect(input_path, output_path)
    addpath('./TCAD_release/MyDetector/');
    addpath('./TCAD_release/Pattern/');
    fprintf('input=%s\n',input_path)
    fprintf('output=%s\n',output_path)
    
    if ~exist(input_path,'dir')
        disp('Input path does not exist')
        return
    end
    
    children = dir(input_path);
    
    results_path = sprintf('%s/%s',output_path,"tcad");
    if ~exist(results_path,'dir')
        mkdir(results_path);
    end

    for i = 1:length(children)
        c = children(i);
        if c.name(1) == '.'
            continue
        end

        if c.isdir
            fprintf('child=%-20s ',c.name)
            results_category = sprintf('%s/%s',results_path,c.name);
            if ~exist(results_category,'dir')
                mkdir(results_category);
            end
            detect_dir(sprintf('%s/%s',c.folder,c.name),results_category);
        end
    end

    disp("Done!!");
    exitcode = 0;
end

function []=detect_dir(input_path, output_path)
     cols = 6;
     rows = 9;
     load('PatternInfo.mat');
     children = dir(input_path);
     for i = 1:length(children)
         c = children(i);
         if c.isdir || (~endsWith(lower(c.name),"jpg") && ~endsWith(lower(c.name),"png"))
             continue;
         end
         try
%              fprintf('reading image %s/%s\n',c.folder,c.name)
             image = imread(sprintf('%s/%s',c.folder,c.name));
             image = im2gray(image);
             [~,file_name,~] = fileparts(c.name);
             
             fid = fopen(sprintf("%s/found_%s",output_path,file_name+".txt"),'w');
             fprintf(fid,"# TCAD 2017 calibration target detector %s\n",c.name);
             fprintf(fid,"image.shape=%d,%d\n",size(image,1),size(image,2));
             
             try
                 tic;
                 [pts, boardSize] = detectMyPatternPoints(image, false);
                 elapsed = toc;
             catch EGDAS
                 disp(EGDAS)
                 fprintf("Detected threw exception %s/%s\n",c.folder,c.name)
                 continue
             end
             
             fprintf(fid,"milliseconds=%.4f\n",elapsed*1000.0);
% 
             if ~isempty(pts)
                 ID = findPtsID( pts,boardSize,squareSize,Pattern,PatternPts,PatternMatrixSize,image,false);
                 fprintf(fid,"markers.size=%d\n",1);
                 fprintf(fid,"marker=%d\n",0);
                 fprintf(fid,"landmarks.size=%d\n",size(pts,1));
                  for i = 1 : size(pts,1)
                      % Put it into the same coordinate system as ground
                      % truth
                      adjusted = ID(i)-1;
                      col = floor(adjusted/rows);
                      row = mod(adjusted,rows);
                      adjusted = row*cols + col;
                      % subtracting 0.5 accounts for the difference between
                      % Matlab and standard image coordinate system
                      fprintf(fid,"%d %.6f %.6f\n",adjusted,pts(i,1)-0.5, pts(i,2)-0.5);
                  end
                fprintf('1');
             else
                fprintf(fid,"markers.size=%d\n",0);
                fprintf('0');
            end

            fclose(fid);
         catch ME
             disp(ME)
             fprintf("Exception processing %s/%s\n",c.folder,c.name)
         end
     end
     fprintf("\n")
end
    