% homo - normalise homogeneous coordinate to have last row equal to 1
%        if last row is zero, it'll return inf.
%
%  Usage:       h = homo(v)
%
%  Arguments:   v - [3 x n] or [4 x n] homogeneous coordinates
%
%  Returns:     h - [3 x n] or [4 x n] homogeneous coordinates
%                   with last row equal to 1 (or 0 for points at infinity)
%
%  Author:      TzuYen Wong
%  Date:        July 2004

function h = homo(v)

if all(v(end,:))
    h = v./(ones(size(v,1),1)*v(end,:));
else
    warning('point at infinity, last row of coordinate CANNNOT be =1')
    vEnd = v(end,:);
    vEnd(find(~vEnd)) = 1;
    h = v./(ones(size(v,1),1)*vEnd);
end
