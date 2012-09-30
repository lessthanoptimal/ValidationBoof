% MINALG_5P6 - constrained minimization algorithm A5.6
%
% Function computes the vector x that minimizes ||Ax|| subject to the 
% condition ||x||=1 and x = G*y, where G has rank r. 
% Implementation of Hartley and Zisserman A5.6 on p595 (2nd Ed) 
%
% Usage:   [x,v] = minAlg_5p6(A,G)
%
% Arguments:
%          A - The constraint matrix, ||Ax|| to be minimized
%          G - The condition matrix, x = G*y
% Returns:
%          x - The vector that minimizes ||Ax|| subject to the 
%              condition ||x||=1 and x = G*v, where G has rank r
%          v - The vector that makes up x = G*v
%                   

% Copyright (c) 2006 TzuYen Wong
% School of Computer Science & Software Engineering
% The University of Western Australia
% http://www.csse.uwa.edu.au/
%
% Created Jan 2006

function [x,v] = minAlg_5p6(A,G)

    % Compute the SVD of G
    [U,D,V] = svd(G,0); % use the economy decomposition
    
    % Extract U2 from U
    r = rank(G);
    U2 = U(:,1:r);
    
    % Find unit vector x2 that minimizes ||A*U2*x2||
    [UU,DD,VV] = svd(A*U2,0); 
    x2 = VV(:,end);
    
    % The required solution
    x = U2*x2;
    
    % Compute the v
    if nargout==2,
        V2 = V(:,1:r);
        D2 = D(1:r,1:r);
        v = V2*inv(D2)*x2;
    end

    return
