% E_FROM_T - computes epipole from trifocal tensor 
%
% Function computes the epipole from trifocal tensor. Refer to Hartley and 
% Zisserman p373 (2nd Ed) for theory and p395 for algorithm
%
% Usage:   [e2,e3] = e_from_T(T)
%
% Arguments:
%          T      - The 3x3x3 trifocal tensor 
%
% Returns:
%          e2, e3 - The epipoles in image 2 and image 3, corresponding to  
%                   the first image camera center
%

% Copyright (c) 2006 TzuYen Wong
% School of Computer Science & Software Engineering
% The University of Western Australia
% http://www.csse.uwa.edu.au/
%
% Created Jan 2006

function [e2,e3] = e_from_T(T)

    e2 = e_from_T1( permute(T,[2 1 3]) );    
    e3 = e_from_T1(T);

%--------------------------------------------------------------------------
% subfunction to retrieve one epipole at a time.    

function e = e_from_T1(T)
        
    for ii = 1:3,
        [U,D,V] = svd(T(:,:,ii),0); % use the economy decomposition

        % Extract epipolar lines from the column of V corresponding to
        % smallest singular value.
        L(ii,:) = V(:,3)';
    end

    [U,D,V] = svd(L,0); % use the economy decomposition

    % Extract epipole from the column of V corresponding to
    % smallest singular value.
    e = homo(V(:,3));
    

    return
