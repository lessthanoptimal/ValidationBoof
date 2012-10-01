% Script for reading in observations and saving the found trifocal tensor to a file

function estimateWongt()
  perfectObs = readTripleObs("../tensor_pixel_perfect.txt");
  noisyObs = readTripleObs("../tensor_pixel_noise.txt");
  
  evaluateTensor(perfectObs',"perfect");
  evaluateTensor(noisyObs',"noise");
  disp("done")
endfunction

%--------------------------------------------------------------------------
% Compute and saves a trifocal tensor computed from the given observations

function evaluateTensor( obs , dataName)
  [T,e2,e3] = triftensor(obs);
  
  fid = fopen(sprintf("wongt_%s_linear7.txt",dataName), 'w');

  fprintf(fid,"# Tensor estimated by TzuYen Wong's matlab code\n")
  
  for i=1:3
    Ti = T(:,:,i);
    for j=1:3
      fprintf(fid,"%.15f %.15f %.15f\n",Ti(j,1),Ti(j,2),Ti(j,3));
    end
    fprintf(fid,"\n");
  end
  fclose(fid);

endfunction

%--------------------------------------------------------------------------
% Reads in observatios

function x = readTripleObs( fileName )
  fid = fopen(fileName, 'r');

  if fid == -1
    disp("can't open requested file")
    return
  end

  x=[];
  
  while( true  )
    s = fgets(fid);
    if s == -1
      break;
    end

    if numel(s) == 0 || s(1) == '#'
      continue;
    end

    v = sscanf(s,"%f %f %f %f %f %f");

    x(end+1,:) = [v(1),v(2),1,v(3),v(4),1,v(5),v(6),1];
  end

  fclose(fid);

endfunction
