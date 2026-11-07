package ir.exam.app.ui.code

/**
 * V349 — جدول زبان‌های محیط کدنویسی (۳۲ زبان).
 * این فایل با scripts/gen_code_langs.py از site/src/code.js ساخته می‌شود؛
 * دستی ویرایش نکنید تا اپ و سایت یکسان بمانند.
 * کد اجرا نمی‌شود: این جدول فقط برای نوشتن، رنگی‌کردن و درج علائم است.
 */
data class CodeLanguage(
    val id: String,
    val name: String,
    val short: String,
    val colorArgb: Long,
    val ext: String,
    val indent: Int,
    val lineComment: String,
    val blockOpen: String,
    val blockClose: String,
    val keywords: List<String>,
    val sample: String,
    val symbols: List<String>,
    val snippets: List<CodeSnippet>
)

/** قطعه‌کد آماده؛ نشان $0 جای نشانگر پس از درج است. */
data class CodeSnippet(val label: String, val code: String)

object CodeLanguages {
    val all: List<CodeLanguage> = listOf(
        CodeLanguage(
            id = "python", name = "پایتون", short = "Py",
            colorArgb = 0xFF3776AB, ext = "py", indent = 4,
            lineComment = "#", blockOpen = "", blockClose = "",
            keywords = listOf("def", "class", "return", "if", "elif", "else", "for", "while", "in", "not", "and", "or", "is", "None", "True", "False", "import", "from", "as", "with", "try", "except", "finally", "raise", "lambda", "yield", "pass", "break", "continue", "global", "nonlocal", "assert", "del", "async", "await", "print"),
            sample = "def greet(name):\n    return f\"سلام {name}\"\n\nprint(greet(\"دنیا\"))\n",
            symbols = listOf(":", "(", ")", "[", "]", "{", "}", "=", "==", "!=", "+", "-", "*", "/", "%", "<", ">", "#", "\"", "'", ",", ".", "_", "|", "&", "\\", "**"),
            snippets = listOf(CodeSnippet("تابع", "def \$0():\n    pass"), CodeSnippet("حلقه for", "for i in range(\$0):\n    "), CodeSnippet("شرط", "if \$0:\n    "), CodeSnippet("کلاس", "class \$0:\n    def __init__(self):\n        pass"), CodeSnippet("چاپ", "print(\$0)"), CodeSnippet("try", "try:\n    \$0\nexcept Exception as e:\n    print(e)"))
        ),
        CodeLanguage(
            id = "javascript", name = "جاوااسکریپت", short = "JS",
            colorArgb = 0xFFF7DF1E, ext = "js", indent = 2,
            lineComment = "//", blockOpen = "/*", blockClose = "*/",
            keywords = listOf("function", "return", "if", "else", "for", "while", "do", "switch", "case", "break", "continue", "const", "let", "var", "class", "new", "this", "typeof", "instanceof", "in", "of", "try", "catch", "finally", "throw", "async", "await", "import", "export", "from", "default", "null", "undefined", "true", "false", "delete", "void", "yield", "extends", "super", "static", "get", "set"),
            sample = "function greet(name) {\n  return `سلام \${name}`;\n}\n\nconsole.log(greet(\"دنیا\"));\n",
            symbols = listOf("(", ")", "{", "}", "[", "]", ";", ":", ",", ".", "=", "==", "!=", "+", "-", "*", "/", "%", "<", ">", "!", "&&", "||", "\"", "'", "`", "\\", "_", "#", "@", "\$", "?"),
            snippets = listOf(CodeSnippet("تابع", "function \$0() {\n  \n}"), CodeSnippet("پیکان", "const \$0 = () => {\n  \n};"), CodeSnippet("حلقه", "for (let i = 0; i < \$0; i++) {\n  \n}"), CodeSnippet("شرط", "if (\$0) {\n  \n}"), CodeSnippet("چاپ", "console.log(\$0);"), CodeSnippet("کلاس", "class \$0 {\n  constructor() {\n    \n  }\n}"))
        ),
        CodeLanguage(
            id = "typescript", name = "تایپ‌اسکریپت", short = "TS",
            colorArgb = 0xFF3178C6, ext = "ts", indent = 2,
            lineComment = "//", blockOpen = "/*", blockClose = "*/",
            keywords = listOf("function", "return", "if", "else", "for", "while", "const", "let", "var", "class", "new", "this", "interface", "type", "enum", "implements", "extends", "public", "private", "protected", "readonly", "static", "async", "await", "import", "export", "from", "as", "null", "undefined", "true", "false", "number", "string", "boolean", "any", "unknown", "never", "void"),
            sample = "function greet(name: string): string {\n  return `سلام \${name}`;\n}\n\nconsole.log(greet(\"دنیا\"));\n",
            symbols = listOf("(", ")", "{", "}", "[", "]", ";", ":", ",", ".", "=", "==", "!=", "+", "-", "*", "/", "%", "<", ">", "!", "&&", "||", "\"", "'", "`", "\\", "_", "#", "@", "\$", "?"),
            snippets = listOf(CodeSnippet("تابع", "function \$0(): void {\n  \n}"), CodeSnippet("رابط", "interface \$0 {\n  \n}"), CodeSnippet("نوع", "type \$0 = {\n  \n};"), CodeSnippet("حلقه", "for (let i = 0; i < \$0; i++) {\n  \n}"), CodeSnippet("چاپ", "console.log(\$0);"))
        ),
        CodeLanguage(
            id = "java", name = "جاوا", short = "Jv",
            colorArgb = 0xFFE76F00, ext = "java", indent = 4,
            lineComment = "//", blockOpen = "/*", blockClose = "*/",
            keywords = listOf("public", "private", "protected", "class", "interface", "extends", "implements", "static", "final", "void", "new", "this", "super", "return", "if", "else", "for", "while", "do", "switch", "case", "break", "continue", "try", "catch", "finally", "throw", "throws", "import", "package", "abstract", "synchronized", "volatile", "transient", "int", "long", "double", "float", "boolean", "char", "byte", "String", "null", "true", "false"),
            sample = "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"سلام دنیا\");\n    }\n}\n",
            symbols = listOf("(", ")", "{", "}", "[", "]", ";", ":", ",", ".", "=", "==", "!=", "+", "-", "*", "/", "%", "<", ">", "!", "&&", "||", "\"", "'", "`", "\\", "_", "#", "@", "\$", "?"),
            snippets = listOf(CodeSnippet("کلاس", "public class \$0 {\n    \n}"), CodeSnippet("main", "public static void main(String[] args) {\n    \$0\n}"), CodeSnippet("متد", "public void \$0() {\n    \n}"), CodeSnippet("حلقه", "for (int i = 0; i < \$0; i++) {\n    \n}"), CodeSnippet("چاپ", "System.out.println(\$0);"))
        ),
        CodeLanguage(
            id = "c", name = "سی", short = "C",
            colorArgb = 0xFF5C6BC0, ext = "c", indent = 4,
            lineComment = "//", blockOpen = "/*", blockClose = "*/",
            keywords = listOf("include", "define", "int", "long", "short", "char", "float", "double", "void", "unsigned", "signed", "signed", "const", "static", "struct", "union", "enum", "typedef", "sizeof", "return", "if", "else", "for", "while", "do", "switch", "case", "break", "continue", "goto", "extern", "register", "volatile", "NULL"),
            sample = "#include <stdio.h>\n\nint main(void) {\n    printf(\"سلام دنیا\\n\");\n    return 0;\n}\n",
            symbols = listOf("(", ")", "{", "}", "[", "]", ";", ":", ",", ".", "=", "==", "!=", "+", "-", "*", "/", "%", "<", ">", "!", "&&", "||", "\"", "'", "`", "\\", "_", "#", "@", "\$", "?"),
            snippets = listOf(CodeSnippet("main", "int main(void) {\n    \$0\n    return 0;\n}"), CodeSnippet("تابع", "void \$0(void) {\n    \n}"), CodeSnippet("حلقه", "for (int i = 0; i < \$0; i++) {\n    \n}"), CodeSnippet("شرط", "if (\$0) {\n    \n}"), CodeSnippet("چاپ", "printf(\"\$0\\n\");"), CodeSnippet("include", "#include <\$0>"))
        ),
        CodeLanguage(
            id = "cpp", name = "سی‌پلاس‌پلاس", short = "C++",
            colorArgb = 0xFF00599C, ext = "cpp", indent = 4,
            lineComment = "//", blockOpen = "/*", blockClose = "*/",
            keywords = listOf("include", "define", "int", "long", "char", "float", "double", "bool", "void", "class", "struct", "public", "private", "protected", "virtual", "override", "new", "delete", "this", "template", "typename", "namespace", "using", "const", "constexpr", "static", "return", "if", "else", "for", "while", "do", "switch", "case", "break", "continue", "try", "catch", "throw", "nullptr", "true", "false", "auto"),
            sample = "#include <iostream>\n\nint main() {\n    std::cout << \"سلام دنیا\" << std::endl;\n    return 0;\n}\n",
            symbols = listOf("(", ")", "{", "}", "[", "]", ";", ":", ",", ".", "=", "==", "!=", "+", "-", "*", "/", "%", "<", ">", "!", "&&", "||", "\"", "'", "`", "\\", "_", "#", "@", "\$", "?"),
            snippets = listOf(CodeSnippet("main", "int main() {\n    \$0\n    return 0;\n}"), CodeSnippet("کلاس", "class \$0 {\npublic:\n    \n};"), CodeSnippet("حلقه", "for (int i = 0; i < \$0; i++) {\n    \n}"), CodeSnippet("چاپ", "std::cout << \$0 << std::endl;"), CodeSnippet("include", "#include <\$0>"))
        ),
        CodeLanguage(
            id = "csharp", name = "سی‌شارپ", short = "C#",
            colorArgb = 0xFF68217A, ext = "cs", indent = 4,
            lineComment = "//", blockOpen = "/*", blockClose = "*/",
            keywords = listOf("using", "namespace", "class", "struct", "interface", "enum", "public", "private", "protected", "internal", "static", "readonly", "const", "void", "var", "new", "this", "base", "return", "if", "else", "for", "foreach", "while", "do", "switch", "case", "break", "continue", "try", "catch", "finally", "throw", "async", "await", "int", "string", "bool", "double", "float", "decimal", "null", "true", "false", "override", "virtual", "sealed", "partial", "get", "set"),
            sample = "using System;\n\nclass Program {\n    static void Main() {\n        Console.WriteLine(\"سلام دنیا\");\n    }\n}\n",
            symbols = listOf("(", ")", "{", "}", "[", "]", ";", ":", ",", ".", "=", "==", "!=", "+", "-", "*", "/", "%", "<", ">", "!", "&&", "||", "\"", "'", "`", "\\", "_", "#", "@", "\$", "?"),
            snippets = listOf(CodeSnippet("کلاس", "class \$0 {\n    \n}"), CodeSnippet("Main", "static void Main() {\n    \$0\n}"), CodeSnippet("متد", "public void \$0() {\n    \n}"), CodeSnippet("حلقه", "foreach (var item in \$0) {\n    \n}"), CodeSnippet("چاپ", "Console.WriteLine(\$0);"))
        ),
        CodeLanguage(
            id = "php", name = "پی‌اچ‌پی", short = "PHP",
            colorArgb = 0xFF777BB4, ext = "php", indent = 4,
            lineComment = "//", blockOpen = "/*", blockClose = "*/",
            keywords = listOf("function", "return", "if", "else", "elseif", "for", "foreach", "while", "do", "switch", "case", "break", "continue", "class", "new", "this", "public", "private", "protected", "static", "const", "echo", "print", "require", "include", "require_once", "include_once", "use", "namespace", "try", "catch", "finally", "throw", "array", "null", "true", "false", "extends", "implements", "interface", "trait"),
            sample = "<?php\n\nfunction greet(string \$name): string {\n    return \"سلام \" . \$name;\n}\n\necho greet(\"دنیا\");\n",
            symbols = listOf("(", ")", "{", "}", "[", "]", ";", ":", ",", ".", "=", "==", "!=", "+", "-", "*", "/", "%", "<", ">", "!", "&&", "||", "\"", "'", "`", "\\", "_", "#", "@", "\$", "?"),
            snippets = listOf(CodeSnippet("تابع", "function \$0() {\n    \n}"), CodeSnippet("حلقه", "foreach (\$0 as \$item) {\n    \n}"), CodeSnippet("شرط", "if (\$0) {\n    \n}"), CodeSnippet("کلاس", "class \$0 {\n    \n}"), CodeSnippet("چاپ", "echo \$0;"))
        ),
        CodeLanguage(
            id = "go", name = "گو", short = "Go",
            colorArgb = 0xFF00ADD8, ext = "go", indent = 4,
            lineComment = "//", blockOpen = "/*", blockClose = "*/",
            keywords = listOf("package", "import", "func", "return", "if", "else", "for", "range", "switch", "case", "default", "break", "continue", "go", "defer", "chan", "select", "map", "struct", "interface", "type", "var", "const", "nil", "true", "false", "string", "int", "int64", "float64", "bool", "byte", "rune", "error", "make", "new", "len", "cap", "append"),
            sample = "package main\n\nimport \"fmt\"\n\nfunc main() {\n    fmt.Println(\"سلام دنیا\")\n}\n",
            symbols = listOf("(", ")", "{", "}", "[", "]", ";", ":", ",", ".", "=", "==", "!=", "+", "-", "*", "/", "%", "<", ">", "!", "&&", "||", "\"", "'", "`", "\\", "_", "#", "@", "\$", "?"),
            snippets = listOf(CodeSnippet("main", "func main() {\n    \$0\n}"), CodeSnippet("تابع", "func \$0() {\n    \n}"), CodeSnippet("ساختار", "type \$0 struct {\n    \n}"), CodeSnippet("حلقه", "for i := 0; i < \$0; i++ {\n    \n}"), CodeSnippet("چاپ", "fmt.Println(\$0)"))
        ),
        CodeLanguage(
            id = "kotlin", name = "کاتلین", short = "Kt",
            colorArgb = 0xFF7F52FF, ext = "kt", indent = 4,
            lineComment = "//", blockOpen = "/*", blockClose = "*/",
            keywords = listOf("fun", "val", "var", "return", "if", "else", "when", "for", "while", "do", "break", "continue", "class", "object", "interface", "data", "sealed", "open", "override", "private", "internal", "public", "protected", "companion", "init", "constructor", "import", "package", "this", "super", "null", "true", "false", "is", "in", "as", "by", "lazy", "suspend", "coroutine", "try", "catch", "finally", "throw", "typealias"),
            sample = "fun greet(name: String): String {\n    return \"سلام \$name\"\n}\n\nfun main() {\n    println(greet(\"دنیا\"))\n}\n",
            symbols = listOf("(", ")", "{", "}", "[", "]", ";", ":", ",", ".", "=", "==", "!=", "+", "-", "*", "/", "%", "<", ">", "!", "&&", "||", "\"", "'", "`", "\\", "_", "#", "@", "\$", "?"),
            snippets = listOf(CodeSnippet("تابع", "fun \$0() {\n    \n}"), CodeSnippet("main", "fun main() {\n    \$0\n}"), CodeSnippet("کلاس", "class \$0 {\n    \n}"), CodeSnippet("داده", "data class \$0(val id: Int)"), CodeSnippet("حلقه", "for (i in 0 until \$0) {\n    \n}"), CodeSnippet("چاپ", "println(\$0)"))
        ),
        CodeLanguage(
            id = "sql", name = "اس‌کیوال", short = "SQL",
            colorArgb = 0xFFE38C00, ext = "sql", indent = 2,
            lineComment = "--", blockOpen = "/*", blockClose = "*/",
            keywords = listOf("select", "from", "where", "insert", "into", "values", "update", "set", "delete", "create", "table", "alter", "drop", "index", "view", "join", "inner", "left", "right", "full", "outer", "on", "group", "by", "order", "having", "limit", "offset", "union", "all", "distinct", "as", "and", "or", "not", "null", "is", "in", "between", "like", "exists", "case", "when", "then", "else", "end", "primary", "key", "foreign", "references", "default", "constraint", "count", "sum", "avg", "min", "max"),
            sample = "SELECT name, score\nFROM students\nWHERE score >= 10\nORDER BY score DESC;\n",
            symbols = listOf("(", ")", ",", ".", ";", "=", "<>", "!=", "<", ">", "*", "+", "-", "'", "\"", "_", "%", "--", "||"),
            snippets = listOf(CodeSnippet("SELECT", "SELECT \$0\nFROM \nWHERE ;"), CodeSnippet("INSERT", "INSERT INTO \$0 ()\nVALUES ();"), CodeSnippet("UPDATE", "UPDATE \$0\nSET \nWHERE ;"), CodeSnippet("JOIN", "SELECT \$0\nFROM a\nJOIN b ON a.id = b.id;"), CodeSnippet("ساخت جدول", "CREATE TABLE \$0 (\n  id INT PRIMARY KEY\n);"), CodeSnippet("گروه", "GROUP BY \$0\nHAVING "))
        ),
        CodeLanguage(
            id = "r", name = "آر", short = "R",
            colorArgb = 0xFF276DC3, ext = "r", indent = 2,
            lineComment = "#", blockOpen = "", blockClose = "",
            keywords = listOf("function", "return", "if", "else", "for", "while", "repeat", "break", "next", "in", "TRUE", "FALSE", "NULL", "NA", "Inf", "NaN", "library", "require", "source", "print", "c", "list", "data.frame", "matrix", "vector", "factor", "length", "nrow", "ncol", "names", "summary", "mean", "median", "sd"),
            sample = "greet <- function(name) {\n  paste(\"سلام\", name)\n}\n\nprint(greet(\"دنیا\"))\n",
            symbols = listOf("(", ")", "[", "]", "{", "}", ";", ":", ",", ".", "=", "==", "+", "-", "*", "/", "%", "<", ">", "!", "&", "|", "\"", "'", "\\", "_", "#", "^"),
            snippets = listOf(CodeSnippet("تابع", "\$0 <- function(x) {\n  \n}"), CodeSnippet("حلقه", "for (i in seq_len(\$0)) {\n  \n}"), CodeSnippet("شرط", "if (\$0) {\n  \n}"), CodeSnippet("چاپ", "print(\$0)"), CodeSnippet("نمودار", "plot(\$0)"))
        ),
        CodeLanguage(
            id = "html", name = "اچ‌تی‌ام‌ال", short = "HTM",
            colorArgb = 0xFFE34F26, ext = "html", indent = 2,
            lineComment = "", blockOpen = "<!--", blockClose = "-->",
            keywords = listOf("html", "head", "body", "title", "meta", "link", "script", "style", "div", "span", "p", "a", "img", "ul", "ol", "li", "table", "thead", "tbody", "tr", "td", "th", "form", "input", "button", "label", "select", "option", "textarea", "h1", "h2", "h3", "h4", "h5", "h6", "header", "footer", "nav", "main", "section", "article", "aside", "figure", "figcaption", "video", "audio", "source", "canvas", "svg", "br", "hr", "strong", "em", "b", "i", "u", "small", "code", "pre", "blockquote", "class", "id", "href", "src", "alt", "type", "name", "value", "placeholder", "required", "disabled", "checked", "readonly", "style", "rel", "charset", "lang", "DOCTYPE"),
            sample = "<!DOCTYPE html>\n<html lang=\"fa\" dir=\"rtl\">\n<head>\n  <meta charset=\"utf-8\">\n  <title>سلام</title>\n</head>\n<body>\n  <h1>سلام دنیا</h1>\n  <p>نخستین صفحهٔ من</p>\n</body>\n</html>\n",
            symbols = listOf("<", ">", "</", "/>", "=", "\"", "'", "(", ")", "{", "}", ";", ":", ",", ".", "!", "-", "#", "&"),
            snippets = listOf(CodeSnippet("صفحه", "<!DOCTYPE html>\n<html lang=\"fa\" dir=\"rtl\">\n<head>\n  <meta charset=\"utf-8\">\n  <title>\$0</title>\n</head>\n<body>\n  \n</body>\n</html>"), CodeSnippet("پیوند", "<a href=\"\$0\"></a>"), CodeSnippet("تصویر", "<img src=\"\$0\" alt=\"\">"), CodeSnippet("فهرست", "<ul>\n  <li>\$0</li>\n</ul>"), CodeSnippet("جدول", "<table>\n  <tr>\n    <td>\$0</td>\n  </tr>\n</table>"), CodeSnippet("فرم", "<form>\n  <input type=\"text\" name=\"\$0\">\n  <button type=\"submit\">ارسال</button>\n</form>"))
        ),
        CodeLanguage(
            id = "css", name = "سی‌اس‌اس", short = "CSS",
            colorArgb = 0xFF1572B6, ext = "css", indent = 2,
            lineComment = "", blockOpen = "/*", blockClose = "*/",
            keywords = listOf("color", "background", "background-color", "font", "font-size", "font-family", "font-weight", "margin", "padding", "border", "border-radius", "width", "height", "min-width", "max-width", "display", "flex", "grid", "gap", "align-items", "justify-content", "position", "top", "right", "bottom", "left", "z-index", "overflow", "text-align", "direction", "content", "transform", "transition", "animation", "opacity", "box-shadow", "cursor", "list-style", "media", "import", "keyframes", "important", "hover", "focus", "active", "root", "before", "after"),
            sample = "body {\n  direction: rtl;\n  font-family: Tahoma, sans-serif;\n  background: #f6f7fb;\n}\n\n.card {\n  padding: 16px;\n  border-radius: 12px;\n  box-shadow: 0 2px 8px rgba(0,0,0,.1);\n}\n",
            symbols = listOf("{", "}", "(", ")", ":", ";", ",", ".", "#", "%", "-", "_", "\"", "'", ">", "*", "/", "@"),
            snippets = listOf(CodeSnippet("قاعده", ".\$0 {\n  \n}"), CodeSnippet("فلکس", "display: flex;\nalign-items: center;\njustify-content: \$0;"), CodeSnippet("گرید", "display: grid;\ngrid-template-columns: \$0;"), CodeSnippet("رسانه", "@media (max-width: 640px) {\n  \$0\n}"), CodeSnippet("گذار", "transition: \$0 .2s ease;"))
        ),
        CodeLanguage(
            id = "rust", name = "راست", short = "Rs",
            colorArgb = 0xFFCE422B, ext = "rs", indent = 4,
            lineComment = "//", blockOpen = "/*", blockClose = "*/",
            keywords = listOf("fn", "let", "mut", "const", "static", "struct", "enum", "impl", "trait", "for", "while", "loop", "if", "else", "match", "return", "break", "continue", "use", "mod", "pub", "crate", "self", "super", "as", "in", "ref", "move", "where", "unsafe", "async", "await", "dyn", "type", "String", "Vec", "Option", "Some", "None", "Result", "Ok", "Err", "i32", "i64", "u32", "u64", "f32", "f64", "bool", "char", "str", "usize", "println"),
            sample = "fn greet(name: &str) -> String {\n    format!(\"سلام {}\", name)\n}\n\nfn main() {\n    println!(\"{}\", greet(\"دنیا\"));\n}\n",
            symbols = listOf("(", ")", "{", "}", "[", "]", ";", ":", ",", ".", "=", "==", "!=", "+", "-", "*", "/", "%", "<", ">", "!", "&&", "||", "\"", "'", "`", "\\", "_", "#", "@", "\$", "?"),
            snippets = listOf(CodeSnippet("تابع", "fn \$0() {\n    \n}"), CodeSnippet("main", "fn main() {\n    \$0\n}"), CodeSnippet("ساختار", "struct \$0 {\n    \n}"), CodeSnippet("حلقه", "for i in 0..\$0 {\n    \n}"), CodeSnippet("تطبیق", "match \$0 {\n    _ => {}\n}"), CodeSnippet("چاپ", "println!(\"{}\", \$0);"))
        ),
        CodeLanguage(
            id = "swift", name = "سوییفت", short = "Sw",
            colorArgb = 0xFFFA7343, ext = "swift", indent = 4,
            lineComment = "//", blockOpen = "/*", blockClose = "*/",
            keywords = listOf("func", "let", "var", "return", "if", "else", "guard", "for", "while", "repeat", "switch", "case", "default", "break", "continue", "class", "struct", "enum", "protocol", "extension", "init", "deinit", "self", "super", "import", "public", "private", "internal", "fileprivate", "static", "override", "final", "throws", "try", "catch", "do", "async", "await", "nil", "true", "false", "Int", "Double", "String", "Bool", "Array", "Dictionary", "Optional", "some", "any", "print"),
            sample = "func greet(name: String) -> String {\n    return \"سلام \\(name)\"\n}\n\nprint(greet(name: \"دنیا\"))\n",
            symbols = listOf("(", ")", "{", "}", "[", "]", ";", ":", ",", ".", "=", "==", "!=", "+", "-", "*", "/", "%", "<", ">", "!", "&&", "||", "\"", "'", "`", "\\", "_", "#", "@", "\$", "?"),
            snippets = listOf(CodeSnippet("تابع", "func \$0() {\n    \n}"), CodeSnippet("کلاس", "class \$0 {\n    \n}"), CodeSnippet("ساختار", "struct \$0 {\n    \n}"), CodeSnippet("حلقه", "for i in 0..<\$0 {\n    \n}"), CodeSnippet("شرط", "if \$0 {\n    \n}"), CodeSnippet("چاپ", "print(\$0)"))
        ),
        CodeLanguage(
            id = "ruby", name = "روبی", short = "Rb",
            colorArgb = 0xFFCC342D, ext = "rb", indent = 2,
            lineComment = "#", blockOpen = "=begin", blockClose = "=end",
            keywords = listOf("def", "end", "class", "module", "return", "if", "elsif", "else", "unless", "while", "until", "for", "in", "do", "then", "case", "when", "begin", "rescue", "ensure", "raise", "yield", "require", "require_relative", "include", "extend", "attr_accessor", "attr_reader", "attr_writer", "self", "super", "nil", "true", "false", "and", "or", "not", "puts", "print", "each", "map", "select", "reduce", "times", "loop", "break", "next", "lambda", "proc"),
            sample = "def greet(name)\n  \"سلام #{name}\"\nend\n\nputs greet(\"دنیا\")\n",
            symbols = listOf(":", "(", ")", "[", "]", "{", "}", "=", "==", "!=", "+", "-", "*", "/", "%", "<", ">", "#", "\"", "'", ",", ".", "_", "|", "&", "\\", "**"),
            snippets = listOf(CodeSnippet("تابع", "def \$0\n  \nend"), CodeSnippet("کلاس", "class \$0\n  def initialize\n    \n  end\nend"), CodeSnippet("حلقه", "\$0.times do |i|\n  \nend"), CodeSnippet("each", "\$0.each do |item|\n  \nend"), CodeSnippet("شرط", "if \$0\n  \nend"), CodeSnippet("چاپ", "puts \$0"))
        ),
        CodeLanguage(
            id = "dart", name = "دارت", short = "Dt",
            colorArgb = 0xFF0175C2, ext = "dart", indent = 2,
            lineComment = "//", blockOpen = "/*", blockClose = "*/",
            keywords = listOf("void", "main", "var", "final", "const", "late", "static", "class", "extends", "implements", "with", "mixin", "abstract", "enum", "typedef", "return", "if", "else", "for", "while", "do", "switch", "case", "break", "continue", "try", "catch", "finally", "throw", "rethrow", "async", "await", "sync", "yield", "import", "export", "library", "part", "show", "hide", "new", "this", "super", "null", "true", "false", "int", "double", "num", "String", "bool", "List", "Map", "Set", "dynamic", "Future", "Stream", "required", "print"),
            sample = "String greet(String name) {\n  return 'سلام \$name';\n}\n\nvoid main() {\n  print(greet('دنیا'));\n}\n",
            symbols = listOf("(", ")", "{", "}", "[", "]", ";", ":", ",", ".", "=", "==", "!=", "+", "-", "*", "/", "%", "<", ">", "!", "&&", "||", "\"", "'", "`", "\\", "_", "#", "@", "\$", "?"),
            snippets = listOf(CodeSnippet("تابع", "void \$0() {\n  \n}"), CodeSnippet("main", "void main() {\n  \$0\n}"), CodeSnippet("کلاس", "class \$0 {\n  \n}"), CodeSnippet("حلقه", "for (var i = 0; i < \$0; i++) {\n  \n}"), CodeSnippet("آینده", "Future<void> \$0() async {\n  \n}"), CodeSnippet("چاپ", "print(\$0);"))
        ),
        CodeLanguage(
            id = "bash", name = "بش / شل", short = "Sh",
            colorArgb = 0xFF4EAA25, ext = "sh", indent = 2,
            lineComment = "#", blockOpen = "", blockClose = "",
            keywords = listOf("if", "then", "else", "elif", "fi", "for", "while", "until", "do", "done", "case", "esac", "in", "function", "return", "break", "continue", "local", "export", "readonly", "declare", "echo", "printf", "read", "exit", "shift", "set", "unset", "source", "alias", "cd", "ls", "cp", "mv", "rm", "mkdir", "rmdir", "cat", "grep", "sed", "awk", "find", "xargs", "chmod", "chown", "tar", "curl", "wget", "git", "sudo", "test", "true", "false"),
            sample = "#!/usr/bin/env bash\n\ngreet() {\n  echo \"سلام \$1\"\n}\n\nfor name in دنیا علی; do\n  greet \"\$name\"\ndone\n",
            symbols = listOf("(", ")", "{", "}", "[", "]", ";", ":", ",", ".", "=", "==", "+", "-", "*", "/", "%", "<", ">", "!", "&", "|", "\"", "'", "\\", "_", "#", "\$", "@"),
            snippets = listOf(CodeSnippet("تابع", "\$0() {\n  \n}"), CodeSnippet("حلقه", "for i in \$0; do\n  \ndone"), CodeSnippet("شرط", "if [ \$0 ]; then\n  \nfi"), CodeSnippet("while", "while [ \$0 ]; do\n  \ndone"), CodeSnippet("چاپ", "echo \"\$0\""), CodeSnippet("متغیر", "\$0=\"\""))
        ),
        CodeLanguage(
            id = "matlab", name = "متلب", short = "Ml",
            colorArgb = 0xFFE16737, ext = "m", indent = 4,
            lineComment = "%", blockOpen = "%{", blockClose = "%}",
            keywords = listOf("function", "end", "return", "if", "elseif", "else", "for", "while", "do", "switch", "case", "otherwise", "break", "continue", "try", "catch", "global", "persistent", "nargin", "nargout", "zeros", "ones", "eye", "linspace", "logspace", "size", "length", "numel", "reshape", "sum", "mean", "median", "std", "min", "max", "sort", "find", "plot", "figure", "hold", "xlabel", "ylabel", "title", "legend", "grid", "disp", "fprintf", "sprintf", "input"),
            sample = "function y = greet(name)\n    % پیام خوش‌آمد\n    y = sprintf('سلام %s', name);\nend\n\nx = linspace(0, 2*pi, 100);\nplot(x, sin(x));\ngrid on;\n",
            symbols = listOf("(", ")", "[", "]", "{", "}", ";", ":", ",", ".", "=", "==", "+", "-", "*", "/", "%", "<", ">", "!", "&", "|", "\"", "'", "\\", "_", "#", "^"),
            snippets = listOf(CodeSnippet("تابع", "function y = \$0(x)\n    \nend"), CodeSnippet("حلقه", "for i = 1:\$0\n    \nend"), CodeSnippet("شرط", "if \$0\n    \nend"), CodeSnippet("نمودار", "plot(\$0);\ngrid on;"), CodeSnippet("چاپ", "fprintf('\$0\\n');"))
        ),
        CodeLanguage(
            id = "asm", name = "اسمبلی", short = "Asm",
            colorArgb = 0xFF6E4C13, ext = "asm", indent = 4,
            lineComment = ";", blockOpen = "", blockClose = "",
            keywords = listOf("section", "global", "extern", "db", "dw", "dd", "dq", "resb", "resw", "resd", "equ", "times", "mov", "movzx", "movsx", "lea", "push", "pop", "add", "sub", "mul", "imul", "div", "idiv", "inc", "dec", "neg", "cmp", "test", "and", "or", "xor", "not", "shl", "shr", "jmp", "je", "jne", "jg", "jl", "jge", "jle", "call", "ret", "leave", "enter", "loop", "nop", "int", "syscall", "eax", "ebx", "ecx", "edx", "rax", "rbx", "rcx", "rdx", "rsi", "rdi", "rsp", "rbp", "esi", "edi", "esp", "ebp"),
            sample = "; برنامهٔ اسمبلی — خروج پیام\nsection .data\n    msg db \"سلام دنیا\", 10\n    len equ \$ - msg\n\nsection .text\n    global _start\n_start:\n    mov rax, 1\n    mov rdi, 1\n    mov rsi, msg\n    mov rdx, len\n    syscall\n    mov rax, 60\n    xor rdi, rdi\n    syscall\n",
            symbols = listOf("(", ")", "{", "}", "[", "]", ";", ":", ",", ".", "=", "==", "+", "-", "*", "/", "%", "<", ">", "!", "&", "|", "\"", "'", "\\", "_", "#", "\$", "@"),
            snippets = listOf(CodeSnippet("بخش داده", "section .data\n    \$0 db 0"), CodeSnippet("بخش کد", "section .text\n    global _start\n_start:\n    \$0"), CodeSnippet("تابع", "\$0:\n    push rbp\n    mov rbp, rsp\n    pop rbp\n    ret"), CodeSnippet("حلقه", "    mov rcx, \$0\nloop_start:\n    dec rcx\n    jnz loop_start"), CodeSnippet("خروج", "    mov rax, 60\n    xor rdi, rdi\n    syscall"))
        ),
        CodeLanguage(
            id = "pascal", name = "پاسکال / دلفی", short = "Pas",
            colorArgb = 0xFFE3F171, ext = "pas", indent = 2,
            lineComment = "//", blockOpen = "{", blockClose = "}",
            keywords = listOf("program", "unit", "uses", "interface", "implementation", "begin", "end", "procedure", "function", "var", "const", "type", "array", "record", "of", "set", "file", "string", "integer", "real", "boolean", "char", "byte", "word", "longint", "if", "then", "else", "case", "while", "do", "repeat", "until", "for", "to", "downto", "break", "continue", "exit", "with", "try", "except", "finally", "raise", "class", "object", "private", "public", "protected", "published", "property", "constructor", "destructor", "inherited", "nil", "true", "false", "and", "or", "not", "div", "mod", "in", "writeln", "write", "readln"),
            sample = "program Hello;\n\nfunction Greet(const name: string): string;\nbegin\n  Greet := 'سلام ' + name;\nend;\n\nbegin\n  WriteLn(Greet('دنیا'));\nend.\n",
            symbols = listOf("(", ")", "[", "]", ";", ":", ":=", ",", ".", "=", "<>", "<", ">", "+", "-", "*", "/", "'", "{", "}", "_", "%"),
            snippets = listOf(CodeSnippet("برنامه", "program \$0;\nbegin\n  \nend."), CodeSnippet("تابع", "function \$0: Integer;\nbegin\n  \nend;"), CodeSnippet("رویه", "procedure \$0;\nbegin\n  \nend;"), CodeSnippet("حلقه", "for i := 1 to \$0 do\nbegin\n  \nend;"), CodeSnippet("شرط", "if \$0 then\nbegin\n  \nend;"), CodeSnippet("چاپ", "WriteLn(\$0);"))
        ),
        CodeLanguage(
            id = "vb", name = "ویژوال بیسیک", short = "VB",
            colorArgb = 0xFF945DB7, ext = "vb", indent = 4,
            lineComment = "'", blockOpen = "", blockClose = "",
            keywords = listOf("Module", "Sub", "Function", "End", "If", "Then", "Else", "ElseIf", "While", "For", "Each", "Next", "To", "Step", "Do", "Loop", "Until", "Select", "Case", "Return", "Dim", "As", "Integer", "String", "Boolean", "Double", "Date", "Object", "New", "Nothing", "True", "False", "And", "Or", "Not", "Mod", "Imports", "Namespace", "Class", "Public", "Private", "Protected", "Shared", "Overrides", "Inherits", "Implements", "Try", "Catch", "Finally", "Throw", "Console", "WriteLine", "ReadLine"),
            sample = "Imports System\n\nModule Program\n    Function Greet(name As String) As String\n        Return \"سلام \" & name\n    End Function\n\n    Sub Main()\n        Console.WriteLine(Greet(\"دنیا\"))\n    End Sub\nEnd Module\n",
            symbols = listOf("(", ")", ";", ":", ",", ".", "=", "<>", "<", ">", "+", "-", "*", "/", "&", "\"", "'", "_", "\\", "%"),
            snippets = listOf(CodeSnippet("ماژول", "Module \$0\n    Sub Main()\n        \n    End Sub\nEnd Module"), CodeSnippet("تابع", "Function \$0() As String\n    Return \"\"\nEnd Function"), CodeSnippet("رویه", "Sub \$0()\n    \nEnd Sub"), CodeSnippet("حلقه", "For i As Integer = 1 To \$0\n    \nNext"), CodeSnippet("شرط", "If \$0 Then\n    \nEnd If"), CodeSnippet("چاپ", "Console.WriteLine(\$0)"))
        ),
        CodeLanguage(
            id = "lua", name = "لوآ", short = "Lua",
            colorArgb = 0xFF000080, ext = "lua", indent = 2,
            lineComment = "--", blockOpen = "--[[", blockClose = "]]",
            keywords = listOf("function", "local", "end", "return", "if", "then", "elseif", "else", "for", "while", "do", "repeat", "until", "break", "in", "and", "or", "not", "nil", "true", "false", "goto", "print", "pairs", "ipairs", "type", "tostring", "tonumber", "table", "string", "math", "io", "os", "require", "setmetatable", "getmetatable", "len", "insert", "remove", "concat", "sort", "format"),
            sample = "local function greet(name)\n  return \"سلام \" .. name\nend\n\nfor i = 1, 3 do\n  print(greet(\"دنیا\"))\nend\n",
            symbols = listOf(":", "(", ")", "[", "]", "{", "}", "=", "==", "!=", "+", "-", "*", "/", "%", "<", ">", "#", "\"", "'", ",", ".", "_", "|", "&", "\\", "**"),
            snippets = listOf(CodeSnippet("تابع", "local function \$0()\n  \nend"), CodeSnippet("حلقه", "for i = 1, \$0 do\n  \nend"), CodeSnippet("شرط", "if \$0 then\n  \nend"), CodeSnippet("جدول", "local \$0 = {}"), CodeSnippet("چاپ", "print(\$0)"))
        ),
        CodeLanguage(
            id = "scala", name = "اسکالا", short = "Sc",
            colorArgb = 0xFFDC322F, ext = "scala", indent = 2,
            lineComment = "//", blockOpen = "/*", blockClose = "*/",
            keywords = listOf("object", "class", "trait", "case", "extends", "with", "abstract", "sealed", "final", "implicit", "lazy", "val", "var", "def", "return", "if", "else", "match", "while", "for", "do", "yield", "try", "catch", "finally", "throw", "new", "this", "super", "import", "package", "override", "private", "protected", "type", "None", "Some", "Option", "List", "Seq", "Map", "Set", "Int", "Double", "String", "Boolean", "Unit", "Any", "AnyRef", "println"),
            sample = "object Main {\n  def greet(name: String): String =\n    s\"سلام \$name\"\n\n  def main(args: Array[String]): Unit =\n    println(greet(\"دنیا\"))\n}\n",
            symbols = listOf("(", ")", "{", "}", "[", "]", ";", ":", ",", ".", "=", "==", "!=", "+", "-", "*", "/", "%", "<", ">", "!", "&&", "||", "\"", "'", "`", "\\", "_", "#", "@", "\$", "?"),
            snippets = listOf(CodeSnippet("شیء", "object \$0 {\n  \n}"), CodeSnippet("تابع", "def \$0(): Unit = {\n  \n}"), CodeSnippet("کلاس", "case class \$0(id: Int)"), CodeSnippet("تطبیق", "\$0 match {\n  case _ => \n}"), CodeSnippet("حلقه", "for (i <- 0 until \$0) {\n  \n}"), CodeSnippet("چاپ", "println(\$0)"))
        ),
        CodeLanguage(
            id = "perl", name = "پرل", short = "Pl",
            colorArgb = 0xFF39457E, ext = "pl", indent = 4,
            lineComment = "#", blockOpen = "=pod", blockClose = "=cut",
            keywords = listOf("my", "our", "local", "sub", "return", "if", "elsif", "else", "unless", "while", "until", "for", "foreach", "do", "last", "next", "redo", "given", "when", "default", "use", "require", "package", "print", "printf", "say", "chomp", "chop", "push", "pop", "shift", "unshift", "splice", "sort", "reverse", "grep", "map", "join", "split", "keys", "values", "each", "defined", "undef", "die", "warn", "ref", "bless", "qw", "qq", "strict", "warnings"),
            sample = "use strict;\nuse warnings;\n\nsub greet {\n    my (\$name) = @_;\n    return \"سلام \$name\";\n}\n\nprint greet(\"دنیا\"), \"\\n\";\n",
            symbols = listOf("(", ")", "{", "}", "[", "]", ";", ":", ",", ".", "=", "==", "+", "-", "*", "/", "%", "<", ">", "!", "&", "|", "\"", "'", "\\", "_", "#", "\$", "@"),
            snippets = listOf(CodeSnippet("تابع", "sub \$0 {\n    my () = @_;\n    \n}"), CodeSnippet("حلقه", "foreach my \$i (\$0) {\n    \n}"), CodeSnippet("شرط", "if (\$0) {\n    \n}"), CodeSnippet("چاپ", "print \"\$0\\n\";"), CodeSnippet("آرایه", "my @\$0 = ();"))
        ),
        CodeLanguage(
            id = "julia", name = "جولیا", short = "Jl",
            colorArgb = 0xFF9558B2, ext = "jl", indent = 4,
            lineComment = "#", blockOpen = "#=", blockClose = "=#",
            keywords = listOf("function", "end", "return", "if", "elseif", "else", "for", "while", "break", "continue", "begin", "let", "local", "global", "const", "struct", "mutable", "abstract", "type", "module", "using", "import", "export", "macro", "quote", "do", "try", "catch", "finally", "throw", "in", "isa", "where", "true", "false", "nothing", "println", "print", "string", "parse", "sum", "mean", "size", "length", "zeros", "ones", "collect", "range"),
            sample = "function greet(name)\n    return \"سلام \$name\"\nend\n\nfor i in 1:3\n    println(greet(\"دنیا\"))\nend\n",
            symbols = listOf("(", ")", "[", "]", "{", "}", ";", ":", ",", ".", "=", "==", "+", "-", "*", "/", "%", "<", ">", "!", "&", "|", "\"", "'", "\\", "_", "#", "^"),
            snippets = listOf(CodeSnippet("تابع", "function \$0()\n    \nend"), CodeSnippet("حلقه", "for i in 1:\$0\n    \nend"), CodeSnippet("شرط", "if \$0\n    \nend"), CodeSnippet("ساختار", "struct \$0\n    \nend"), CodeSnippet("چاپ", "println(\$0)"))
        ),
        CodeLanguage(
            id = "fortran", name = "فرترن", short = "F90",
            colorArgb = 0xFF734F96, ext = "f90", indent = 2,
            lineComment = "!", blockOpen = "", blockClose = "",
            keywords = listOf("program", "end", "module", "contains", "subroutine", "function", "implicit", "none", "integer", "real", "double", "precision", "complex", "logical", "character", "parameter", "dimension", "allocatable", "allocate", "deallocate", "intent", "in", "out", "inout", "if", "then", "else", "elseif", "endif", "do", "enddo", "while", "select", "case", "where", "forall", "return", "call", "print", "write", "read", "format", "open", "close", "stop", "cycle", "exit", "use", "only"),
            sample = "program hello\n  implicit none\n  integer :: i\n\n  do i = 1, 3\n    print *, \"سلام دنیا\"\n  end do\nend program hello\n",
            symbols = listOf("(", ")", "[", "]", "{", "}", ";", ":", ",", ".", "=", "==", "+", "-", "*", "/", "%", "<", ">", "!", "&", "|", "\"", "'", "\\", "_", "#", "^"),
            snippets = listOf(CodeSnippet("برنامه", "program \$0\n  implicit none\n  \nend program"), CodeSnippet("تابع", "function \$0(x) result(y)\n  implicit none\n  \nend function"), CodeSnippet("حلقه", "do i = 1, \$0\n    \n  end do"), CodeSnippet("شرط", "if (\$0) then\n    \n  end if"), CodeSnippet("چاپ", "print *, \$0"))
        ),
        CodeLanguage(
            id = "prolog", name = "پرولوگ", short = "Pro",
            colorArgb = 0xFFF1471D, ext = "pro", indent = 2,
            lineComment = "%", blockOpen = "/*", blockClose = "*/",
            keywords = listOf("assert", "asserta", "assertz", "retract", "findall", "bagof", "setof", "forall", "once", "call", "not", "fail", "true", "is", "mod", "rem", "atom", "number", "integer", "float", "atomic", "length", "append", "member", "reverse", "sort", "msort", "last", "nth0", "select", "permutation", "write", "writeln", "nl", "format", "read", "consult", "dynamic", "discontiguous", "multifile", "op"),
            sample = "% رابطهٔ پدر و فرزند\nparent(ali, reza).\nparent(reza, sara).\n\nancestor(X, Y) :- parent(X, Y).\nancestor(X, Y) :- parent(X, Z), ancestor(Z, Y).\n\n% پرسش: ?- ancestor(ali, sara).\n",
            symbols = listOf("(", ")", "[", "]", "{", "}", "'", "`", ",", ":", ".", "\"", ";", "#", "+", "-", "*", "/", "=", "<", ">", "_", "%"),
            snippets = listOf(CodeSnippet("واقعیت", "\$0(ali, reza)."), CodeSnippet("قاعده", "\$0(X, Y) :-\n    parent(X, Y)."), CodeSnippet("پرسش", "?- \$0(X)."), CodeSnippet("فهرست", "member(\$0, [1, 2, 3])."), CodeSnippet("چاپ", "write(\$0), nl."))
        ),
        CodeLanguage(
            id = "lisp", name = "لیسپ / اسکیم", short = "Lsp",
            colorArgb = 0xFF3FB68B, ext = "lisp", indent = 2,
            lineComment = ";", blockOpen = "#|", blockClose = "|#",
            keywords = listOf("defun", "defvar", "defparameter", "let", "let*", "lambda", "if", "cond", "when", "unless", "case", "do", "dolist", "dotimes", "loop", "progn", "setq", "setf", "quote", "function", "car", "cdr", "cons", "list", "append", "length", "reverse", "mapcar", "reduce", "apply", "funcall", "format", "print", "princ", "princ-to-string", "null", "atom", "listp", "numberp", "eq", "equal", "not", "and", "or"),
            sample = "(defun greet (name)\n  \"پیام خوش‌آمد\"\n  (format nil \"سلام ~a\" name))\n\n(dolist (who '(\"دنیا\" \"علی\"))\n  (print (greet who)))\n",
            symbols = listOf("(", ")", "[", "]", "{", "}", "'", "`", ",", ":", ".", "\"", ";", "#", "+", "-", "*", "/", "=", "<", ">", "_", "%"),
            snippets = listOf(CodeSnippet("تابع", "(defun \$0 (x)\n  )"), CodeSnippet("متغیر", "(defvar *\$0* nil)"), CodeSnippet("let", "(let ((\$0 1))\n  )"), CodeSnippet("شرط", "(if \$0\n    t\n    nil)"), CodeSnippet("حلقه", "(dolist (x \$0)\n  (print x))"), CodeSnippet("چاپ", "(print \$0)"))
        ),
        CodeLanguage(
            id = "groovy", name = "گرووی", short = "Gv",
            colorArgb = 0xFF4298B8, ext = "groovy", indent = 4,
            lineComment = "//", blockOpen = "/*", blockClose = "*/",
            keywords = listOf("def", "class", "interface", "trait", "extends", "implements", "return", "if", "else", "for", "while", "do", "switch", "case", "break", "continue", "try", "catch", "finally", "throw", "new", "this", "super", "import", "package", "static", "final", "abstract", "private", "public", "protected", "void", "null", "true", "false", "in", "as", "instanceof", "String", "Integer", "List", "Map", "println", "print", "each", "collect", "find", "findAll", "with"),
            sample = "def greet(String name) {\n    \"سلام \${name}\"\n}\n\n[\"دنیا\", \"علی\"].each {\n    println greet(it)\n}\n",
            symbols = listOf("(", ")", "{", "}", "[", "]", ";", ":", ",", ".", "=", "==", "!=", "+", "-", "*", "/", "%", "<", ">", "!", "&&", "||", "\"", "'", "`", "\\", "_", "#", "@", "\$", "?"),
            snippets = listOf(CodeSnippet("تابع", "def \$0() {\n    \n}"), CodeSnippet("کلاس", "class \$0 {\n    \n}"), CodeSnippet("each", "\$0.each {\n    \n}"), CodeSnippet("حلقه", "for (i in 0..<\$0) {\n    \n}"), CodeSnippet("چاپ", "println \$0"))
        ),
        CodeLanguage(
            id = "objc", name = "آبجکتیو-سی", short = "ObjC",
            colorArgb = 0xFF438EFF, ext = "m", indent = 4,
            lineComment = "//", blockOpen = "/*", blockClose = "*/",
            keywords = listOf("import", "interface", "implementation", "end", "protocol", "property", "synthesize", "dynamic", "class", "struct", "enum", "typedef", "void", "int", "float", "double", "char", "BOOL", "id", "instancetype", "NSString", "NSArray", "NSDictionary", "NSMutableArray", "NSInteger", "CGFloat", "self", "super", "nil", "YES", "NO", "return", "if", "else", "for", "while", "do", "switch", "case", "break", "continue", "try", "catch", "finally", "throw", "alloc", "init", "new", "strong", "weak", "copy", "nonatomic", "assign", "retain", "release", "autorelease", "NSLog"),
            sample = "#import <Foundation/Foundation.h>\n\n@interface Greeter : NSObject\n- (NSString *)greet:(NSString *)name;\n@end\n\n@implementation Greeter\n- (NSString *)greet:(NSString *)name {\n    return [NSString stringWithFormat:@\"سلام %@\", name];\n}\n@end\n",
            symbols = listOf("(", ")", "{", "}", "[", "]", ";", ":", ",", ".", "=", "==", "!=", "+", "-", "*", "/", "%", "<", ">", "!", "&&", "||", "\"", "'", "`", "\\", "_", "#", "@", "\$", "?"),
            snippets = listOf(CodeSnippet("رابط", "@interface \$0 : NSObject\n@end"), CodeSnippet("پیاده‌سازی", "@implementation \$0\n@end"), CodeSnippet("متد", "- (void)\$0 {\n    \n}"), CodeSnippet("حلقه", "for (int i = 0; i < \$0; i++) {\n    \n}"), CodeSnippet("چاپ", "NSLog(@\"\$0\");"))
        )
    )

    private val byId: Map<String, CodeLanguage> = all.associateBy { it.id }

    /** نام‌های جایگزینی که ممکن است در بلوک کد نوشته شده باشد. */
    private val alias: Map<String, String> = mapOf(
        "py" to "python", "python3" to "python", "js" to "javascript", "node" to "javascript",
        "ts" to "typescript", "c++" to "cpp", "cxx" to "cpp", "c#" to "csharp", "cs" to "csharp",
        "kt" to "kotlin", "kts" to "kotlin", "golang" to "go", "postgres" to "sql",
        "postgresql" to "sql", "mysql" to "sql", "sqlite" to "sql", "rlang" to "r",
        "htm" to "html", "html5" to "html", "scss" to "css", "sass" to "css", "rs" to "rust",
        "rb" to "ruby", "shell" to "bash", "sh" to "bash", "zsh" to "bash", "octave" to "matlab",
        "assembly" to "asm", "nasm" to "asm", "pas" to "pascal", "delphi" to "pascal",
        "vb.net" to "vb", "vbnet" to "vb", "basic" to "vb", "sc" to "scala", "pl" to "perl",
        "jl" to "julia", "f90" to "fortran", "f95" to "fortran", "swipl" to "prolog",
        "scheme" to "lisp", "clisp" to "lisp", "elisp" to "lisp", "gvy" to "groovy",
        "objective-c" to "objc", "objectivec" to "objc", "obj-c" to "objc"
    )

    val default: CodeLanguage get() = byId.getValue("python")

    fun byIdOr(id: String?): CodeLanguage {
        val key = id?.trim()?.lowercase().orEmpty()
        return byId[key] ?: byId[alias[key]] ?: default
    }
}
