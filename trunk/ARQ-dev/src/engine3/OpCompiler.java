/*
 * (c) Copyright 2007 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package engine3;

import java.util.Stack;

import com.hp.hpl.jena.query.core.ARQNotImplemented;
import com.hp.hpl.jena.query.core.ElementBasicGraphPattern;
import com.hp.hpl.jena.query.engine.Binding0;
import com.hp.hpl.jena.query.engine.QueryIterator;
import com.hp.hpl.jena.query.engine1.ExecutionContext;
import com.hp.hpl.jena.query.engine1.PlanElement;
import com.hp.hpl.jena.query.engine1.plan.PlanBasicGraphPattern;
import com.hp.hpl.jena.query.engine2.op.*;

import engine3.iterators.QueryIterFilterExpr;
import engine3.iterators.QueryIterOptionalIndex;
import engine3.iterators.QueryIterSingleton;

public class OpCompiler
{
    static QueryIterator compile(Op op, ExecutionContext execCxt)
    {
        OpCompilerVisitor v = new OpCompilerVisitor(execCxt) ;
        return v.compile(op) ;
    }
    
    static class OpCompilerVisitor implements OpVisitor
    {
        QueryIterator queryIterator = null ;
        
        ExecutionContext execCxt ;
        Stack stack = new Stack() ;
        
        public OpCompilerVisitor(ExecutionContext execCxt)
        { this.execCxt = execCxt ; } 
        
        public QueryIterator compile(Op op)
        {
            return compile(op, null) ;
        }

        public QueryIterator compile(Op op, QueryIterator previous)
        {
            op.visit(this) ;
            QueryIterator qIter = pop() ;
//            if ( previous != null )
//                qIter.setInput(previous) ;
            return qIter ;
        }
        
        public void visit(OpBGP opBGP)
        {
            ElementBasicGraphPattern bgp = new ElementBasicGraphPattern() ; 
            bgp.getTriples().addAll(opBGP.getPattern()) ;
            
            // Turn into a real PlanBasicGraphPattern (with property function sorting out)
            PlanElement planElt = PlanBasicGraphPattern.make(execCxt.getContext(), bgp) ;
            QueryIterator start = popOrRoot() ;
            QueryIterator qIter = planElt.build(start, execCxt) ;
            push(qIter) ;
        }

        // Zero inputs.
        public void visit(OpQuadPattern quadPattern)
        {}

        // Two inputs.
        public void visit(OpJoin opJoin)
        {
            QueryIterator start = popOrRoot() ;
            
            QueryIterator left = compile(opJoin.getLeft(), start) ;
            QueryIterator right = compile(opJoin.getRight(), left) ;
        }

        public void visit(OpLeftJoin opLeftJoin)
        {
            QueryIterator left = compile(opLeftJoin.getLeft(), popOrRoot()) ;
            // Do an indexed substitute into the right if possible
            boolean canDoLinear = false ; 

            if ( canDoLinear )
            {
                QueryIterator qIter = new QueryIterOptionalIndex(left, opLeftJoin.getRight(), execCxt) ;
                push(qIter) ;
            }
            // Do by sub-evaluation
            
            QueryIterator right = compile(opLeftJoin.getRight(), popOrRoot()) ;
            // return an iterator that does the optional here.
            QueryIterator qIter = null ;
            if ( true )
                throw new ARQNotImplemented("Exhaustive optional evaluation") ;
            push(qIter) ;
            
        }
        
        public void visit(OpUnion opUnion)
        {
            //QueryIterConcat cat = new QueryIterConcat(execCxt) ;
        }

        public void visit(OpFilter opFilter)
        {
            Op sub = opFilter.getSubOp() ;
            
            // Put filter in best place
            if ( sub instanceof OpBGP )
            {}
            
            if ( sub instanceof OpQuadPattern )
            {}
            
            QueryIterator qIter = compile(sub) ;
            qIter = new QueryIterFilterExpr(qIter, opFilter.getExpr(), execCxt) ;
        }

        public void visit(OpGraph opGraph)
        {}

        public void visit(OpDatasetNames dsNames)
        {}

        public void visit(OpTable opTable)
        {}

        public void visit(OpExt opExt)
        {}

        public void visit(OpOrder opOrder)
        {}

        public void visit(OpProject opProject)
        {}

        public void visit(OpDistinct opDistinct)
        {}

        public void visit(OpSlice opSlice)
        {}
    
//        private void startLinear(QueryIterator qIter)
//        { current = qIter ; }
//        
//        private void endLinear()
//        { current = null ; }
//        
//        private void addToLinear(QueryIterator qIter)
//        {
//            if ( current == null )
//                throw new ARQInternalErrorException("OpCompiler: addToLinear - linear section not started") ;
//            // seed qiter with current
//            current = qIter ;
//        }
        
        private void push(QueryIterator qIter)  { stack.push(qIter) ; }
        private QueryIterator pop()            { return (QueryIterator)stack.pop() ; }
        
        private QueryIterator popOrRoot()
        { 
            if ( stack.size() == 0 )
                return root() ;
            return (QueryIterator)stack.pop() ;
        }
        
        private QueryIterator root()
        {
            return new QueryIterSingleton(new Binding0(), execCxt) ;
        }
    }
}

/*
 * (c) Copyright 2007 Hewlett-Packard Development Company, LP
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */