% TRIFTENSOR - computes trifocal tensor from 7 or more points
%
% Function computes the trifocal tensor from 7 or more matching points in
% three images.  This code follows the normalised linear algorithm and 
% algebraic minimization algorithm given by Hartley and Zisserman 
% p393 (2nd Ed).
%
% Usage:   [T, e2, e3] = triftensor(x1, x2, x3)
%          [T, e2, e3] = triftensor(x)
%
% Arguments:
%          x1, x2, x3 - Three sets of corresponding 3xN set of homogeneous
%          points.
%         
%          x      - If a single argument is supplied it is assumed that it
%                   is in the form x = [x1; x2; x3]
% Returns:
%          T      - The 3x3x3 trifocal tensor such that 
%                   x1^i * x2^j * x3^k * eps_jpr * eps_kqs * T_i^pq = 0
%                   OR [x2]_x * SUM_i(x1^i*T_i) * [x3]_x = 0_(3x3)
%          e2, e3 - The epipoles in image 2 and image 3, corresponding to  
%                   the first image camera center
%

% Copyright (c) 2006 TzuYen Wong
% wongt AT csse DOT uwa DOT edu DOT au
% School of Computer Science & Software Engineering
% The University of Western Australia
% http://www.csse.uwa.edu.au/
% 
% Permission is hereby granted, free of charge, to any person obtaining a copy
% of this software and associated documentation files (the "Software"), to deal
% in the Software without restriction, subject to the following conditions:
% 
% The above copyright notice and this permission notice shall be included in 
% all copies or substantial portions of the Software.
%
% The Software is provided "as is", without warranty of any kind.

% created Jan 2006

function [T,e2,e3] = triftensor(varargin)
    
    [x1, x2, x3, npts] = checkargs(varargin(:));
    
    % Normalise each set of points so that the origin 
    % is at centroid and mean distance from origin is sqrt(2). 
    % normalise2dpts also ensures the scale parameter is 1.
    [x1, H1] = normalise2dpts(x1);
    [x2, H2] = normalise2dpts(x2);
    [x3, H3] = normalise2dpts(x3);
    
    % Build the constraint matrix
    % each point correspondance gives 4 linearly independant equations
    %   by changing the I=1,2 and L=1,2
    A = zeros(4*npts,27);
    n = 0;
    for I = 1:2, 
        for L = 1:2,
            n = n+1;
            r = [ (n-1)*npts+1 : n*npts ];
            c1 = 3*(I-1)+L; % = [1  2  4  5]
            c2 = 3*(I-1)+3; % = [3  3  6  6] 
            c3 = L+6;       % = [7  8  7  8]
            c4 = 9;         % = [9  9  9  9]
            c = [0 9 18];

            A(r, c1+c) =  x1';
            A(r, c2+c) = -x1' .* ( x3(L,:)' * ones(1,3) );
            A(r, c3+c) = -x1' .* ( x2(I,:)' * ones(1,3) );
            A(r, c4+c) =  x1' .* ( x2(I,:)'.*x3(L,:)' * ones(1,3) );
    
        end
    end
    
 	[U,D,V] = svd(A,0); % use the economy decomposition

    % Extract trifocal tensor from the column of V corresponding to
    % smallest singular value.
    t0 = V(:,27);
    T0 = permute( reshape(t0,3,3,3) , [2 1 3] );
    
    % Ensure that that trifocal tensor is geomatrically valid by retrieving 
    % its epipoles and performing algebraic minimization
    [e2,e3] = e_from_T(T0);
    E = E_from_ee(e2,e3); % subfunction
    t = minAlg_5p6(A,E);
    T = permute( reshape(t,3,3,3) , [2 1 3] );
    
    % Denormalise
    % T_i^jk = H1_i^r * (inv(H2))_s^j * (inv(H3))_t^k * Tcap_r^st

    for ii = 1:3,
        Y(:,:,ii) = inv(H2) * T(:,:,ii) * (inv(H3))';
    end
    
    for ii = 1:3,
        T_denorm(:,:,ii) =   H1(1,ii)*Y(:,:,1) ...
                           + H1(2,ii)*Y(:,:,2) ...
                           + H1(3,ii)*Y(:,:,3);
    end
    
    T = T_denorm;
    
    return
%--------------------------------------------------------------------------
% Function to check argument values and set defaults

function [x1, x2, x3, npts] = checkargs(arg);
    
    if length(arg) == 3
        x1 = arg{1};
        x2 = arg{2};
        x3 = arg{3};
        if ~all(size(x1)==size(x2) & size(x1)==size(x3))
            error('x1, x2 and x3 must have the same size');
        elseif size(x1,1) ~= 3
            error('x1, x2 and x3 must be 3xN');
        end
        
    elseif length(arg) == 1
        if size(arg{1},1) ~= 9
            error('Single argument x must be 9xN');
        else
            x1 = arg{1}(1:3,:);
            x2 = arg{1}(4:6,:);
            x3 = arg{1}(7:9,:);
        end
    else
        error('Wrong number of arguments supplied');
    end
      
    npts = size(x1,2);
    if npts < 7
        error('At least 7 points are needed to compute the trifocal tensor');
    end
    
    return
    
%--------------------------------------------------------------------------
% Function to build the relationship matrix which represent
% T_i^jk = a_i^j * b_4^k  -  a_4^j * b_i^k  
% as t = E * aa, where aa = [a'(:) ; b'(:)], (note: for representation only)

function E = E_from_ee(e2,e3)

    
    e2Block = [ diag([-e2(1)*ones(1,3)])
                diag([-e2(2)*ones(1,3)])
                diag([-e2(3)*ones(1,3)])];
            
    e3Block = zeros(9,3);
    e3Block(1:3,1) = e3;
    e3Block(4:6,2) = e3;
    e3Block(7:9,3) = e3;
    
    E = zeros(27,18);
    E( 1: 9,[1:3,10:12]) = [e3Block e2Block];
    E(10:18,[4:6,13:15]) = [e3Block e2Block];
    E(19:27,[7:9,16:18]) = [e3Block e2Block];
    
    return