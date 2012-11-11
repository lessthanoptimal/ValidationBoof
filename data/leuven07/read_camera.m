function [CamInt CamRot CamPos GP] = read_camera( sPath, fmin, fmax )

if nargin==0 | isempty(sPath)
  sPath = ['left/maps'];
end;
if nargin<=1       
  fmin = 0;
  fmax = 2349;
elseif nargin==2
  fmax = fmin;
end;

CamInt = [];
CamRot = [];
CamPos = [];
GP     = [];

k=1;
for i=fmin:fmax;
  if i-fmin>k*100,
    disp( sprintf('\bFinished %d%%...\n', round(100*(i-fmin)/(fmax-fmin))) );
    k = k+1;
  end;
  % read the internal camera calibration matrix
  [s x] = system(sprintf('cat %s/camera.%05d | head -n 3', ...
                         sPath,i ));
  CamInt = [CamInt; str2num(x)];
  
  % read the camera rotation matrix
  [s x] = system(sprintf('cat %s/camera.%05d | tail -n 8 | head -n 3', ...
                         sPath,i ));
  CamRot = [CamRot; str2num(x)];
  
  % read the camera position
  [s x] = system(sprintf('cat %s/camera.%05d | tail -n 4 | head -n 1', ...
                         sPath,i ));
  CamPos = [CamPos; str2num(x)];
  
  % read the ground plane
  [s x] = system(sprintf('cat %s/camera.%05d | tail -n 2 | head -n 1', ...
                         sPath,i ));
  GP = [GP; str2num(x)];  
end;
