package ir.exam.app.core.math

sealed interface MathNode {
    data class Sequence(val children:List<MathNode>):MathNode
    data class Symbol(val value:String,val bold:Boolean=false):MathNode
    data class Fraction(val top:MathNode,val bottom:MathNode):MathNode
    data class Radical(val body:MathNode,val index:MathNode?=null):MathNode
    data class Script(val base:MathNode,val upper:MathNode?,val lower:MathNode?):MathNode
    data class Matrix(val rows:List<List<MathNode>>,val delimiter:Char='['):MathNode
    data class Accent(val body:MathNode,val mark:String):MathNode
}

/** parser محدود اما ساختاری برای فرمول‌های آموزشی؛ هیچ HTML یا WebView اجرا نمی‌کند. */
object NativeMathParser {
    private val symbols=mapOf(
        "alpha" to "α","beta" to "β","gamma" to "γ","delta" to "δ","epsilon" to "ε","theta" to "θ","lambda" to "λ","mu" to "μ","pi" to "π","rho" to "ρ","sigma" to "σ","phi" to "φ","omega" to "ω",
        "Delta" to "Δ","Sigma" to "Σ","Omega" to "Ω","times" to "×","div" to "÷","pm" to "±","mp" to "∓","leq" to "≤","geq" to "≥","neq" to "≠","approx" to "≈","infty" to "∞","sum" to "∑","prod" to "∏","int" to "∫","partial" to "∂","nabla" to "∇","rightarrow" to "→","leftarrow" to "←","Rightarrow" to "⇒","in" to "∈","notin" to "∉","subset" to "⊂","subseteq" to "⊆","cup" to "∪","cap" to "∩","cdot" to "·","degree" to "°","forall" to "∀","exists" to "∃","therefore" to "∴"
    )
    fun parse(tex:String):MathNode {
        require(tex.length<=8000){"فرمول بیش از حد بلند است."}
        return Parser(tex).parseSequence()
    }
    private class Parser(private val s:String){var i=0
        fun parseSequence(stop:Char?=null):MathNode{
            val out=mutableListOf<MathNode>()
            while(i<s.length && (stop==null||s[i]!=stop)){
                var node=atom()
                var up:MathNode?=null;var low:MathNode?=null
                while(i<s.length&&(s[i]=='^'||s[i]=='_')){val m=s[i++];val n=groupOrAtom();if(m=='^')up=n else low=n}
                if(up!=null||low!=null)node=MathNode.Script(node,up,low)
                out+=node
            }
            if(stop!=null&&i<s.length&&s[i]==stop)i++
            return if(out.size==1)out[0] else MathNode.Sequence(out)
        }
        private fun atom():MathNode{
            if(i>=s.length)return MathNode.Symbol("")
            return when(val c=s[i++]){
                '{'->parseSequence('}')
                '\\'->command()
                else->MathNode.Symbol(c.toString())
            }
        }
        private fun groupOrAtom():MathNode{while(i<s.length&&s[i].isWhitespace())i++;return if(i<s.length&&s[i]=='{'){i++;parseSequence('}')}else atom()}
        private fun command():MathNode{
            val start=i;while(i<s.length&&s[i].isLetter())i++;val name=s.substring(start,i)
            if(name.isEmpty()&&i<s.length)return MathNode.Symbol(s[i++].toString())
            return when(name){
                "frac"->MathNode.Fraction(groupOrAtom(),groupOrAtom())
                "sqrt"->MathNode.Radical(groupOrAtom())
                "mathbf","bold","boldsymbol"->MathNode.Symbol(flat(groupOrAtom()),true)
                "mathrm","text","operatorname","mathbb","mathcal"->MathNode.Symbol(flat(groupOrAtom()))
                "hat"->MathNode.Accent(groupOrAtom(),"ˆ")
                "bar","overline"->MathNode.Accent(groupOrAtom(),"¯")
                "vec"->MathNode.Accent(groupOrAtom(),"→")
                "dot"->MathNode.Accent(groupOrAtom(),"˙")
                "left","right"->if(i<s.length)MathNode.Symbol(s[i++].toString())else MathNode.Symbol("")
                "begin"->parseEnvironment()
                else->MathNode.Symbol(symbols[name]?:name)
            }
        }
        private fun parseEnvironment():MathNode{
            val env=flat(groupOrAtom());val end="\\end{$env}";val at=s.indexOf(end,i)
            if(at<0)return MathNode.Symbol(env)
            val body=s.substring(i,at);i=at+end.length
            if(env in setOf("matrix","bmatrix","pmatrix","vmatrix","cases","aligned","align")){
                val rows=body.split("\\\\").map{row->row.split('&').map{parse(it)}}
                return MathNode.Matrix(rows,when(env){"pmatrix"->'(';"vmatrix"->'|';"cases"->'{';else->'['})
            }
            return MathNode.Symbol(body)
        }
        private fun flat(n:MathNode):String=when(n){is MathNode.Symbol->n.value;is MathNode.Sequence->n.children.joinToString(""){flat(it)};else->""}
    }
}
