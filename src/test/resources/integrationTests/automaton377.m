% MATLAB/Octave output
% The row vector v denotes the indicator vector of the (singleton)
% set of initial states.
v = [1 0 0 0 0 0 0 0 0 0];

% In what follows, the M_i_x, for a free variable i and a value x, denotes
% an incidence matrix of the underlying graph of (the automaton of)
% the predicate in the query.
% For every pair of states p and q, the entry M_i_x[p][q] denotes the number of
% transitions with i=x from p to q.

M_length_abc_0 = [1 0 0 0 0 0 0 0 0 0; 0 0 0 1 1 0 0 0 0 0; 0 0 0 0 0 1 0 0 0 0; 0 0 0 1 1 0 0 0 0 0; 0 0 0 1 0 0 0 0 0 0; 0 0 0 0 0 0 0 1 1 0; 0 0 0 1 0 0 0 0 0 0; 0 0 0 0 0 0 0 0 0 0; 0 0 0 0 0 1 0 0 0 0; 0 0 0 0 1 1 0 0 0 0];

M_length_abc_1 = [0 1 1 0 0 0 0 0 0 0; 0 0 0 0 0 0 0 0 0 0; 0 0 0 0 0 0 0 0 0 0; 0 1 0 0 0 0 1 0 0 0; 0 1 0 0 0 0 0 0 0 0; 0 0 0 0 0 0 1 0 0 1; 0 0 0 0 0 0 0 0 0 0; 0 0 1 0 0 0 0 0 0 0; 0 1 0 0 0 0 0 0 0 0; 0 0 0 0 0 0 0 0 0 0];

% The column vector w denotes the indicator vector of the
% set of final states.
w = [1; 1; 1; 1; 1; 1; 1; 0; 1; 1];

% fix up v by multiplying
for i = 1:size(v, 2), v = v * M_length_abc_0; end
