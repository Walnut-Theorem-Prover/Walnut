# SageMath output
# The row vector v denotes the indicator vector of the (singleton)
# set of initial states.
v = matrix(ZZ, 1, 12, [1,0,0,0,0,0,0,0,0,0,0,0])

# In what follows, the M_i_x, for a free variable i and a value x, denotes
# an incidence matrix of the underlying graph of (the automaton of)
# the predicate in the query.
# For every pair of states p and q, the entry M_i_x[p][q] denotes the number of
# transitions with i=x from p to q.

M_n_0 = matrix(ZZ, 12, 12, [[1,0,0,0,0,0,0,0,0,0,0,0],[0,0,0,1,1,0,0,0,0,0,0,0],[0,0,0,0,0,1,0,0,0,0,0,0],[0,0,0,0,0,0,1,0,0,0,0,0],[0,0,0,0,0,0,0,1,0,0,0,0],[0,0,0,0,0,0,0,0,1,0,0,0],[0,0,0,0,0,0,0,0,0,1,0,0],[0,0,0,0,1,0,0,0,0,0,0,0],[0,0,0,0,0,1,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0,1,0,0],[0,0,0,0,1,0,0,0,0,0,0,0]])

M_n_1 = matrix(ZZ, 12, 12, [[0,1,1,0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0,0,0,0],[0,1,1,0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0,0,1,0],[0,0,1,0,0,0,0,0,0,0,0,1],[0,0,0,0,0,0,0,0,0,0,1,0],[0,0,0,0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0,0,0,0]])

# The column vector w denotes the indicator vector of the
# set of final states.
w = matrix(ZZ, 12, 1, [0,0,0,0,0,0,0,1,0,1,1,0])

# fix up v by multiplying
for _ in range(v.ncols()):
    v = v * M_n_0
