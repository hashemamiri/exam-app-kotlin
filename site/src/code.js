/* ===================================================================
   V348 — ابزار کدنویسی (محیط شبیه‌سازی ویرایشگر)

   چه هست: یک ویرایشگر کد با رنگی‌شدن دستورها، شمارهٔ خط، تورفتگی خودکار و
   انتخاب زبان. استاد در متن سؤال کد می‌گذارد و دانش‌آموز در پاسخ کد می‌نویسد.

   چه نیست: کد اجرا نمی‌شود. این فقط محیط نوشتن و نمایش است؛ تصحیح مثل
   سؤال تشریحی دستی انجام می‌شود. هیچ کدی به سرور فرستاده یا اجرا نمی‌شود.

   قالب ذخیره‌سازی: بلوک استاندارد ``` که هم برای انسان خواناست و هم اگر
   جایی به‌صورت متن خام نشان داده شود (مثلاً اپ اندروید) بی‌معنی نمی‌شود:
       ```python
       print("سلام")
       ```
   به همین دلیل هیچ تغییر پایگاه داده لازم نیست.
   =================================================================== */
(function () {
  var S = window.ExamSite;
  if (!S) return;
  var el = S.el, esc = S.esc, toast = S.toast;
  var MAX = 20000; /* سقف نویسهٔ کد تا پاسخ‌ها و چاپ سنگین نشوند */

  /* زبان‌ها: نام، نشان کوتاه، رنگ، پسوند، توضیح کوتاه و نمونهٔ آغاز */
  var LANGS = [
    {id: 'python', name: 'پایتون', short: 'Py', color: '#3776AB', ext: 'py', indent: 4, line: '#', block: null,
     kw: 'def class return if elif else for while in not and or is None True False import from as with try except finally raise lambda yield pass break continue global nonlocal assert del async await print',
     sample: 'def greet(name):\n    return f"سلام {name}"\n\nprint(greet("دنیا"))\n'},
    {id: 'javascript', name: 'جاوااسکریپت', short: 'JS', color: '#F7DF1E', ext: 'js', indent: 2, line: '//', block: ['/*', '*/'],
     kw: 'function return if else for while do switch case break continue const let var class new this typeof instanceof in of try catch finally throw async await import export from default null undefined true false delete void yield extends super static get set',
     sample: 'function greet(name) {\n  return `سلام ${name}`;\n}\n\nconsole.log(greet("دنیا"));\n'},
    {id: 'typescript', name: 'تایپ‌اسکریپت', short: 'TS', color: '#3178C6', ext: 'ts', indent: 2, line: '//', block: ['/*', '*/'],
     kw: 'function return if else for while const let var class new this interface type enum implements extends public private protected readonly static async await import export from as null undefined true false number string boolean any unknown never void',
     sample: 'function greet(name: string): string {\n  return `سلام ${name}`;\n}\n\nconsole.log(greet("دنیا"));\n'},
    {id: 'java', name: 'جاوا', short: 'Jv', color: '#E76F00', ext: 'java', indent: 4, line: '//', block: ['/*', '*/'],
     kw: 'public private protected class interface extends implements static final void new this super return if else for while do switch case break continue try catch finally throw throws import package abstract synchronized volatile transient int long double float boolean char byte String null true false',
     sample: 'public class Main {\n    public static void main(String[] args) {\n        System.out.println("سلام دنیا");\n    }\n}\n'},
    {id: 'c', name: 'سی', short: 'C', color: '#5C6BC0', ext: 'c', indent: 4, line: '//', block: ['/*', '*/'],
     kw: 'include define int long short char float double void unsigned signed signed const static struct union enum typedef sizeof return if else for while do switch case break continue goto extern register volatile NULL',
     sample: '#include <stdio.h>\n\nint main(void) {\n    printf("سلام دنیا\\n");\n    return 0;\n}\n'},
    {id: 'cpp', name: 'سی‌پلاس‌پلاس', short: 'C++', color: '#00599C', ext: 'cpp', indent: 4, line: '//', block: ['/*', '*/'],
     kw: 'include define int long char float double bool void class struct public private protected virtual override new delete this template typename namespace using const constexpr static return if else for while do switch case break continue try catch throw nullptr true false auto',
     sample: '#include <iostream>\n\nint main() {\n    std::cout << "سلام دنیا" << std::endl;\n    return 0;\n}\n'},
    {id: 'csharp', name: 'سی‌شارپ', short: 'C#', color: '#68217A', ext: 'cs', indent: 4, line: '//', block: ['/*', '*/'],
     kw: 'using namespace class struct interface enum public private protected internal static readonly const void var new this base return if else for foreach while do switch case break continue try catch finally throw async await int string bool double float decimal null true false override virtual sealed partial get set',
     sample: 'using System;\n\nclass Program {\n    static void Main() {\n        Console.WriteLine("سلام دنیا");\n    }\n}\n'},
    {id: 'php', name: 'پی‌اچ‌پی', short: 'PHP', color: '#777BB4', ext: 'php', indent: 4, line: '//', block: ['/*', '*/'],
     kw: 'function return if else elseif for foreach while do switch case break continue class new this public private protected static const echo print require include require_once include_once use namespace try catch finally throw array null true false extends implements interface trait',
     sample: '<?php\n\nfunction greet(string $name): string {\n    return "سلام " . $name;\n}\n\necho greet("دنیا");\n'},
    {id: 'go', name: 'گو', short: 'Go', color: '#00ADD8', ext: 'go', indent: 4, line: '//', block: ['/*', '*/'],
     kw: 'package import func return if else for range switch case default break continue go defer chan select map struct interface type var const nil true false string int int64 float64 bool byte rune error make new len cap append',
     sample: 'package main\n\nimport "fmt"\n\nfunc main() {\n    fmt.Println("سلام دنیا")\n}\n'},
    {id: 'kotlin', name: 'کاتلین', short: 'Kt', color: '#7F52FF', ext: 'kt', indent: 4, line: '//', block: ['/*', '*/'],
     kw: 'fun val var return if else when for while do break continue class object interface data sealed open override private internal public protected companion init constructor import package this super null true false is in as by lazy suspend coroutine try catch finally throw typealias',
     sample: 'fun greet(name: String): String {\n    return "سلام $name"\n}\n\nfun main() {\n    println(greet("دنیا"))\n}\n'},
    {id: 'sql', name: 'اس‌کیوال', short: 'SQL', color: '#E38C00', ext: 'sql', indent: 2, line: '--', block: ['/*', '*/'],
     kw: 'select from where insert into values update set delete create table alter drop index view join inner left right full outer on group by order having limit offset union all distinct as and or not null is in between like exists case when then else end primary key foreign references default constraint count sum avg min max',
     sample: 'SELECT name, score\nFROM students\nWHERE score >= 10\nORDER BY score DESC;\n'},
    {id: 'r', name: 'آر', short: 'R', color: '#276DC3', ext: 'r', indent: 2, line: '#', block: null,
     kw: 'function return if else for while repeat break next in TRUE FALSE NULL NA Inf NaN library require source print c list data.frame matrix vector factor length nrow ncol names summary mean median sd',
     sample: 'greet <- function(name) {\n  paste("سلام", name)\n}\n\nprint(greet("دنیا"))\n'},
    {id: 'html', name: 'اچ‌تی‌ام‌ال', short: 'HTM', color: '#E34F26', ext: 'html', indent: 2, line: null, block: ['<!--', '-->'],
     kw: 'html head body title meta link script style div span p a img ul ol li table thead tbody tr td th form input button label select option textarea h1 h2 h3 h4 h5 h6 header footer nav main section article aside figure figcaption video audio source canvas svg br hr strong em b i u small code pre blockquote class id href src alt type name value placeholder required disabled checked readonly style rel charset lang DOCTYPE',
     sample: '<!DOCTYPE html>\n<html lang="fa" dir="rtl">\n<head>\n  <meta charset="utf-8">\n  <title>سلام</title>\n</head>\n<body>\n  <h1>سلام دنیا</h1>\n  <p>نخستین صفحهٔ من</p>\n</body>\n</html>\n'},
    {id: 'css', name: 'سی‌اس‌اس', short: 'CSS', color: '#1572B6', ext: 'css', indent: 2, line: null, block: ['/*', '*/'],
     kw: 'color background background-color font font-size font-family font-weight margin padding border border-radius width height min-width max-width display flex grid gap align-items justify-content position top right bottom left z-index overflow text-align direction content transform transition animation opacity box-shadow cursor list-style media import keyframes important hover focus active root before after',
     sample: 'body {\n  direction: rtl;\n  font-family: Tahoma, sans-serif;\n  background: #f6f7fb;\n}\n\n.card {\n  padding: 16px;\n  border-radius: 12px;\n  box-shadow: 0 2px 8px rgba(0,0,0,.1);\n}\n'},
    {id: 'rust', name: 'راست', short: 'Rs', color: '#CE422B', ext: 'rs', indent: 4, line: '//', block: ['/*', '*/'],
     kw: 'fn let mut const static struct enum impl trait for while loop if else match return break continue use mod pub crate self super as in ref move where unsafe async await dyn type String Vec Option Some None Result Ok Err i32 i64 u32 u64 f32 f64 bool char str usize println',
     sample: 'fn greet(name: &str) -> String {\n    format!("سلام {}", name)\n}\n\nfn main() {\n    println!("{}", greet("دنیا"));\n}\n'},
    {id: 'swift', name: 'سوییفت', short: 'Sw', color: '#FA7343', ext: 'swift', indent: 4, line: '//', block: ['/*', '*/'],
     kw: 'func let var return if else guard for while repeat switch case default break continue class struct enum protocol extension init deinit self super import public private internal fileprivate static override final throws try catch do async await nil true false Int Double String Bool Array Dictionary Optional some any print',
     sample: 'func greet(name: String) -> String {\n    return "سلام \\(name)"\n}\n\nprint(greet(name: "دنیا"))\n'},
    {id: 'ruby', name: 'روبی', short: 'Rb', color: '#CC342D', ext: 'rb', indent: 2, line: '#', block: ['=begin', '=end'],
     kw: 'def end class module return if elsif else unless while until for in do then case when begin rescue ensure raise yield require require_relative include extend attr_accessor attr_reader attr_writer self super nil true false and or not puts print each map select reduce times loop break next lambda proc',
     sample: 'def greet(name)\n  "سلام #{name}"\nend\n\nputs greet("دنیا")\n'},
    {id: 'dart', name: 'دارت', short: 'Dt', color: '#0175C2', ext: 'dart', indent: 2, line: '//', block: ['/*', '*/'],
     kw: 'void main var final const late static class extends implements with mixin abstract enum typedef return if else for while do switch case break continue try catch finally throw rethrow async await sync yield import export library part show hide new this super null true false int double num String bool List Map Set dynamic Future Stream required print',
     sample: 'String greet(String name) {\n  return \'سلام $name\';\n}\n\nvoid main() {\n  print(greet(\'دنیا\'));\n}\n'},
    {id: 'bash', name: 'بش / شل', short: 'Sh', color: '#4EAA25', ext: 'sh', indent: 2, line: '#', block: null,
     kw: 'if then else elif fi for while until do done case esac in function return break continue local export readonly declare echo printf read exit shift set unset source alias cd ls cp mv rm mkdir rmdir cat grep sed awk find xargs chmod chown tar curl wget git sudo test true false',
     sample: '#!/usr/bin/env bash\n\ngreet() {\n  echo "سلام $1"\n}\n\nfor name in دنیا علی; do\n  greet "$name"\ndone\n'},
    {id: 'matlab', name: 'متلب', short: 'Ml', color: '#E16737', ext: 'm', indent: 4, line: '%', block: ['%{', '%}'],
     kw: 'function end return if elseif else for while do switch case otherwise break continue try catch global persistent nargin nargout zeros ones eye linspace logspace size length numel reshape sum mean median std min max sort find plot figure hold xlabel ylabel title legend grid disp fprintf sprintf input',
     sample: 'function y = greet(name)\n    % پیام خوش‌آمد\n    y = sprintf(\'سلام %s\', name);\nend\n\nx = linspace(0, 2*pi, 100);\nplot(x, sin(x));\ngrid on;\n'},
    {id: 'asm', name: 'اسمبلی', short: 'Asm', color: '#6E4C13', ext: 'asm', indent: 4, line: ';', block: null,
     kw: 'section global extern db dw dd dq resb resw resd equ times mov movzx movsx lea push pop add sub mul imul div idiv inc dec neg cmp test and or xor not shl shr jmp je jne jg jl jge jle call ret leave enter loop nop int syscall eax ebx ecx edx rax rbx rcx rdx rsi rdi rsp rbp esi edi esp ebp',
     sample: '; برنامهٔ اسمبلی — خروج پیام\nsection .data\n    msg db "سلام دنیا", 10\n    len equ $ - msg\n\nsection .text\n    global _start\n_start:\n    mov rax, 1\n    mov rdi, 1\n    mov rsi, msg\n    mov rdx, len\n    syscall\n    mov rax, 60\n    xor rdi, rdi\n    syscall\n'},
    {id: 'pascal', name: 'پاسکال / دلفی', short: 'Pas', color: '#E3F171', ext: 'pas', indent: 2, line: '//', block: ['{', '}'],
     kw: 'program unit uses interface implementation begin end procedure function var const type array record of set file string integer real boolean char byte word longint if then else case while do repeat until for to downto break continue exit with try except finally raise class object private public protected published property constructor destructor inherited nil true false and or not div mod in writeln write readln',
     sample: 'program Hello;\n\nfunction Greet(const name: string): string;\nbegin\n  Greet := \'سلام \' + name;\nend;\n\nbegin\n  WriteLn(Greet(\'دنیا\'));\nend.\n'},
    {id: 'vb', name: 'ویژوال بیسیک', short: 'VB', color: '#945DB7', ext: 'vb', indent: 4, line: "'", block: null,
     kw: 'Module Sub Function End If Then Else ElseIf While For Each Next To Step Do Loop Until Select Case Return Dim As Integer String Boolean Double Date Object New Nothing True False And Or Not Mod Imports Namespace Class Public Private Protected Shared Overrides Inherits Implements Try Catch Finally Throw Console WriteLine ReadLine',
     sample: 'Imports System\n\nModule Program\n    Function Greet(name As String) As String\n        Return "سلام " & name\n    End Function\n\n    Sub Main()\n        Console.WriteLine(Greet("دنیا"))\n    End Sub\nEnd Module\n'},
    {id: 'lua', name: 'لوآ', short: 'Lua', color: '#000080', ext: 'lua', indent: 2, line: '--', block: ['--[[', ']]'],
     kw: 'function local end return if then elseif else for while do repeat until break in and or not nil true false goto print pairs ipairs type tostring tonumber table string math io os require setmetatable getmetatable len insert remove concat sort format',
     sample: 'local function greet(name)\n  return "سلام " .. name\nend\n\nfor i = 1, 3 do\n  print(greet("دنیا"))\nend\n'},
    {id: 'scala', name: 'اسکالا', short: 'Sc', color: '#DC322F', ext: 'scala', indent: 2, line: '//', block: ['/*', '*/'],
     kw: 'object class trait case extends with abstract sealed final implicit lazy val var def return if else match while for do yield try catch finally throw new this super import package override private protected type None Some Option List Seq Map Set Int Double String Boolean Unit Any AnyRef println',
     sample: 'object Main {\n  def greet(name: String): String =\n    s"سلام $name"\n\n  def main(args: Array[String]): Unit =\n    println(greet("دنیا"))\n}\n'},
    {id: 'perl', name: 'پرل', short: 'Pl', color: '#39457E', ext: 'pl', indent: 4, line: '#', block: ['=pod', '=cut'],
     kw: 'my our local sub return if elsif else unless while until for foreach do last next redo given when default use require package print printf say chomp chop push pop shift unshift splice sort reverse grep map join split keys values each defined undef die warn ref bless qw qq strict warnings',
     sample: 'use strict;\nuse warnings;\n\nsub greet {\n    my ($name) = @_;\n    return "سلام $name";\n}\n\nprint greet("دنیا"), "\\n";\n'},
    {id: 'julia', name: 'جولیا', short: 'Jl', color: '#9558B2', ext: 'jl', indent: 4, line: '#', block: ['#=', '=#'],
     kw: 'function end return if elseif else for while break continue begin let local global const struct mutable abstract type module using import export macro quote do try catch finally throw in isa where true false nothing println print string parse sum mean size length zeros ones collect range',
     sample: 'function greet(name)\n    return "سلام $name"\nend\n\nfor i in 1:3\n    println(greet("دنیا"))\nend\n'},
    {id: 'fortran', name: 'فرترن', short: 'F90', color: '#734F96', ext: 'f90', indent: 2, line: '!', block: null,
     kw: 'program end module contains subroutine function implicit none integer real double precision complex logical character parameter dimension allocatable allocate deallocate intent in out inout if then else elseif endif do enddo while select case where forall return call print write read format open close stop cycle exit use only',
     sample: 'program hello\n  implicit none\n  integer :: i\n\n  do i = 1, 3\n    print *, "سلام دنیا"\n  end do\nend program hello\n'},
    {id: 'prolog', name: 'پرولوگ', short: 'Pro', color: '#F1471D', ext: 'pro', indent: 2, line: '%', block: ['/*', '*/'],
     kw: 'assert asserta assertz retract findall bagof setof forall once call not fail true is mod rem atom number integer float atomic length append member reverse sort msort last nth0 select permutation write writeln nl format read consult dynamic discontiguous multifile op',
     sample: '% رابطهٔ پدر و فرزند\nparent(ali, reza).\nparent(reza, sara).\n\nancestor(X, Y) :- parent(X, Y).\nancestor(X, Y) :- parent(X, Z), ancestor(Z, Y).\n\n% پرسش: ?- ancestor(ali, sara).\n'},
    {id: 'lisp', name: 'لیسپ / اسکیم', short: 'Lsp', color: '#3FB68B', ext: 'lisp', indent: 2, line: ';', block: ['#|', '|#'],
     kw: 'defun defvar defparameter let let* lambda if cond when unless case do dolist dotimes loop progn setq setf quote function car cdr cons list append length reverse mapcar reduce apply funcall format print princ princ-to-string null atom listp numberp eq equal not and or',
     sample: '(defun greet (name)\n  "پیام خوش‌آمد"\n  (format nil "سلام ~a" name))\n\n(dolist (who \'("دنیا" "علی"))\n  (print (greet who)))\n'},
    {id: 'groovy', name: 'گرووی', short: 'Gv', color: '#4298B8', ext: 'groovy', indent: 4, line: '//', block: ['/*', '*/'],
     kw: 'def class interface trait extends implements return if else for while do switch case break continue try catch finally throw new this super import package static final abstract private public protected void null true false in as instanceof String Integer List Map println print each collect find findAll with',
     sample: 'def greet(String name) {\n    "سلام ${name}"\n}\n\n["دنیا", "علی"].each {\n    println greet(it)\n}\n'},
    {id: 'objc', name: 'آبجکتیو-سی', short: 'ObjC', color: '#438EFF', ext: 'm', indent: 4, line: '//', block: ['/*', '*/'],
     kw: 'import interface implementation end protocol property synthesize dynamic class struct enum typedef void int float double char BOOL id instancetype NSString NSArray NSDictionary NSMutableArray NSInteger CGFloat self super nil YES NO return if else for while do switch case break continue try catch finally throw alloc init new strong weak copy nonatomic assign retain release autorelease NSLog',
     sample: '#import <Foundation/Foundation.h>\n\n@interface Greeter : NSObject\n- (NSString *)greet:(NSString *)name;\n@end\n\n@implementation Greeter\n- (NSString *)greet:(NSString *)name {\n    return [NSString stringWithFormat:@"سلام %@", name];\n}\n@end\n'}
  ];
  var BY_ID = {};
  LANGS.forEach(function (L) { BY_ID[L.id] = L; });
  /* نام‌های جایگزین که کاربر ممکن است در بلوک بنویسد */
  var ALIAS = {py: 'python', 'python3': 'python', js: 'javascript', node: 'javascript', ts: 'typescript',
    'c++': 'cpp', cxx: 'cpp', cc: 'cpp', 'c#': 'csharp', cs: 'csharp', dotnet: 'csharp', kt: 'kotlin', kts: 'kotlin',
    golang: 'go', postgres: 'sql', postgresql: 'sql', mysql: 'sql', sqlite: 'sql', php8: 'php', rlang: 'r',
    htm: 'html', html5: 'html', scss: 'css', sass: 'css', rs: 'rust', 'rustlang': 'rust', 'swift5': 'swift',
    rb: 'ruby', shell: 'bash', sh: 'bash', zsh: 'bash', 'shell script': 'bash', octave: 'matlab',
    assembly: 'asm', nasm: 'asm', x86: 'asm', pas: 'pascal', delphi: 'pascal', freepascal: 'pascal',
    'vb.net': 'vb', vbnet: 'vb', basic: 'vb', 'visual basic': 'vb', 'lua5': 'lua', sc: 'scala',
    pl: 'perl', 'perl5': 'perl', jl: 'julia', f90: 'fortran', f95: 'fortran', 'fortran90': 'fortran',
    swipl: 'prolog', 'pro': 'prolog', scheme: 'lisp', clisp: 'lisp', 'common lisp': 'lisp', elisp: 'lisp',
    gvy: 'groovy', 'objective-c': 'objc', objectivec: 'objc', 'obj-c': 'objc', mm: 'objc'};
  function lang(id) {
    var k = String(id || '').trim().toLowerCase();
    return BY_ID[k] || BY_ID[ALIAS[k]] || BY_ID.python;
  }

  /* ---------------- V349: محیط اختصاصی هر زبان ----------------
     سه چیز محیط را برای هر زبان ویژه می‌کند:
       ۱) نوار علائم: چیدن دکمه‌ها به ترتیبی که در آن زبان بیشتر لازم می‌شود
       ۲) دکمه‌های آماده: قطعه‌کدهای پرکاربرد همان زبان
       ۳) کامل‌کردن خودکار: واژه‌های همان زبان هنگام تایپ
     هیچ‌کدام کد را اجرا نمی‌کنند؛ فقط درج متن‌اند. */

  /* خانوادهٔ نگارشی زبان‌ها — مبنای چیدمان نوار علائم */
  var SYM_FAMILY = {
    c:      ['(', ')', '{', '}', '[', ']', ';', ':', ',', '.', '=', '==', '!=', '+', '-', '*', '/', '%', '<', '>', '!', '&&', '||', '"', "'", '`', '\\', '_', '#', '@', '$', '?'],
    colon:  [':', '(', ')', '[', ']', '{', '}', '=', '==', '!=', '+', '-', '*', '/', '%', '<', '>', '#', '"', "'", ',', '.', '_', '|', '&', '\\', '**'],
    query:  ['(', ')', ',', '.', ';', '=', '<>', '!=', '<', '>', '*', '+', '-', "'", '"', '_', '%', '--', '||'],
    markup: ['<', '>', '</', '/>', '=', '"', "'", '(', ')', '{', '}', ';', ':', ',', '.', '!', '-', '#', '&'],
    style:  ['{', '}', '(', ')', ':', ';', ',', '.', '#', '%', '-', '_', '"', "'", '>', '*', '/', '@'],
    shell:  ['(', ')', '{', '}', '[', ']', ';', ':', ',', '.', '=', '==', '+', '-', '*', '/', '%', '<', '>', '!', '&', '|', '"', "'", '\\', '_', '#', '$', '@'],
    numeric:['(', ')', '[', ']', '{', '}', ';', ':', ',', '.', '=', '==', '+', '-', '*', '/', '%', '<', '>', '!', '&', '|', '"', "'", '\\', '_', '#', '^'],
    lisp:   ['(', ')', '[', ']', '{', '}', "'", '`', ',', ':', '.', '"', ';', '#', '+', '-', '*', '/', '=', '<', '>', '_', '%'],
    pascal: ['(', ')', '[', ']', ';', ':', ':=', ',', '.', '=', '<>', '<', '>', '+', '-', '*', '/', "'", '{', '}', '_', '%'],
    basic:  ['(', ')', ';', ':', ',', '.', '=', '<>', '<', '>', '+', '-', '*', '/', '&', '"', "'", '_', '\\', '%']
  };
  var LANG_FAMILY = {
    python: 'colon', ruby: 'colon',
    javascript: 'c', typescript: 'c', java: 'c', c: 'c', cpp: 'c', csharp: 'c', php: 'c', go: 'c',
    kotlin: 'c', rust: 'c', swift: 'c', dart: 'c', scala: 'c', groovy: 'c', objc: 'c',
    sql: 'query', html: 'markup', css: 'style',
    bash: 'shell', perl: 'shell',
    r: 'numeric', matlab: 'numeric', julia: 'numeric', fortran: 'numeric',
    lisp: 'lisp', prolog: 'lisp', pascal: 'pascal', vb: 'basic', lua: 'colon', asm: 'shell'
  };
  function symbolsFor(id) {
    var L = lang(id);
    return SYM_FAMILY[LANG_FAMILY[L.id] || 'c'].slice();
  }

  /* دکمه‌های آمادهٔ هر زبان. نشان $0 جای نشانگر پس از درج است. */
  var SNIPPETS = {
    python: [['تابع', 'def $0():\n    pass'], ['حلقه for', 'for i in range($0):\n    '], ['شرط', 'if $0:\n    '], ['کلاس', 'class $0:\n    def __init__(self):\n        pass'], ['چاپ', 'print($0)'], ['try', 'try:\n    $0\nexcept Exception as e:\n    print(e)']],
    javascript: [['تابع', 'function $0() {\n  \n}'], ['پیکان', 'const $0 = () => {\n  \n};'], ['حلقه', 'for (let i = 0; i < $0; i++) {\n  \n}'], ['شرط', 'if ($0) {\n  \n}'], ['چاپ', 'console.log($0);'], ['کلاس', 'class $0 {\n  constructor() {\n    \n  }\n}']],
    typescript: [['تابع', 'function $0(): void {\n  \n}'], ['رابط', 'interface $0 {\n  \n}'], ['نوع', 'type $0 = {\n  \n};'], ['حلقه', 'for (let i = 0; i < $0; i++) {\n  \n}'], ['چاپ', 'console.log($0);']],
    java: [['کلاس', 'public class $0 {\n    \n}'], ['main', 'public static void main(String[] args) {\n    $0\n}'], ['متد', 'public void $0() {\n    \n}'], ['حلقه', 'for (int i = 0; i < $0; i++) {\n    \n}'], ['چاپ', 'System.out.println($0);']],
    c: [['main', 'int main(void) {\n    $0\n    return 0;\n}'], ['تابع', 'void $0(void) {\n    \n}'], ['حلقه', 'for (int i = 0; i < $0; i++) {\n    \n}'], ['شرط', 'if ($0) {\n    \n}'], ['چاپ', 'printf("$0\\n");'], ['include', '#include <$0>']],
    cpp: [['main', 'int main() {\n    $0\n    return 0;\n}'], ['کلاس', 'class $0 {\npublic:\n    \n};'], ['حلقه', 'for (int i = 0; i < $0; i++) {\n    \n}'], ['چاپ', 'std::cout << $0 << std::endl;'], ['include', '#include <$0>']],
    csharp: [['کلاس', 'class $0 {\n    \n}'], ['Main', 'static void Main() {\n    $0\n}'], ['متد', 'public void $0() {\n    \n}'], ['حلقه', 'foreach (var item in $0) {\n    \n}'], ['چاپ', 'Console.WriteLine($0);']],
    php: [['تابع', 'function $0() {\n    \n}'], ['حلقه', 'foreach ($0 as $item) {\n    \n}'], ['شرط', 'if ($0) {\n    \n}'], ['کلاس', 'class $0 {\n    \n}'], ['چاپ', 'echo $0;']],
    go: [['main', 'func main() {\n    $0\n}'], ['تابع', 'func $0() {\n    \n}'], ['ساختار', 'type $0 struct {\n    \n}'], ['حلقه', 'for i := 0; i < $0; i++ {\n    \n}'], ['چاپ', 'fmt.Println($0)']],
    kotlin: [['تابع', 'fun $0() {\n    \n}'], ['main', 'fun main() {\n    $0\n}'], ['کلاس', 'class $0 {\n    \n}'], ['داده', 'data class $0(val id: Int)'], ['حلقه', 'for (i in 0 until $0) {\n    \n}'], ['چاپ', 'println($0)']],
    sql: [['SELECT', 'SELECT $0\nFROM \nWHERE ;'], ['INSERT', 'INSERT INTO $0 ()\nVALUES ();'], ['UPDATE', 'UPDATE $0\nSET \nWHERE ;'], ['JOIN', 'SELECT $0\nFROM a\nJOIN b ON a.id = b.id;'], ['ساخت جدول', 'CREATE TABLE $0 (\n  id INT PRIMARY KEY\n);'], ['گروه', 'GROUP BY $0\nHAVING ']],
    r: [['تابع', '$0 <- function(x) {\n  \n}'], ['حلقه', 'for (i in seq_len($0)) {\n  \n}'], ['شرط', 'if ($0) {\n  \n}'], ['چاپ', 'print($0)'], ['نمودار', 'plot($0)']],
    html: [['صفحه', '<!DOCTYPE html>\n<html lang="fa" dir="rtl">\n<head>\n  <meta charset="utf-8">\n  <title>$0</title>\n</head>\n<body>\n  \n</body>\n</html>'], ['پیوند', '<a href="$0"></a>'], ['تصویر', '<img src="$0" alt="">'], ['فهرست', '<ul>\n  <li>$0</li>\n</ul>'], ['جدول', '<table>\n  <tr>\n    <td>$0</td>\n  </tr>\n</table>'], ['فرم', '<form>\n  <input type="text" name="$0">\n  <button type="submit">ارسال</button>\n</form>']],
    css: [['قاعده', '.$0 {\n  \n}'], ['فلکس', 'display: flex;\nalign-items: center;\njustify-content: $0;'], ['گرید', 'display: grid;\ngrid-template-columns: $0;'], ['رسانه', '@media (max-width: 640px) {\n  $0\n}'], ['گذار', 'transition: $0 .2s ease;']],
    rust: [['تابع', 'fn $0() {\n    \n}'], ['main', 'fn main() {\n    $0\n}'], ['ساختار', 'struct $0 {\n    \n}'], ['حلقه', 'for i in 0..$0 {\n    \n}'], ['تطبیق', 'match $0 {\n    _ => {}\n}'], ['چاپ', 'println!("{}", $0);']],
    swift: [['تابع', 'func $0() {\n    \n}'], ['کلاس', 'class $0 {\n    \n}'], ['ساختار', 'struct $0 {\n    \n}'], ['حلقه', 'for i in 0..<$0 {\n    \n}'], ['شرط', 'if $0 {\n    \n}'], ['چاپ', 'print($0)']],
    ruby: [['تابع', 'def $0\n  \nend'], ['کلاس', 'class $0\n  def initialize\n    \n  end\nend'], ['حلقه', '$0.times do |i|\n  \nend'], ['each', '$0.each do |item|\n  \nend'], ['شرط', 'if $0\n  \nend'], ['چاپ', 'puts $0']],
    dart: [['تابع', 'void $0() {\n  \n}'], ['main', 'void main() {\n  $0\n}'], ['کلاس', 'class $0 {\n  \n}'], ['حلقه', 'for (var i = 0; i < $0; i++) {\n  \n}'], ['آینده', 'Future<void> $0() async {\n  \n}'], ['چاپ', 'print($0);']],
    bash: [['تابع', '$0() {\n  \n}'], ['حلقه', 'for i in $0; do\n  \ndone'], ['شرط', 'if [ $0 ]; then\n  \nfi'], ['while', 'while [ $0 ]; do\n  \ndone'], ['چاپ', 'echo "$0"'], ['متغیر', '$0=""']],
    matlab: [['تابع', 'function y = $0(x)\n    \nend'], ['حلقه', 'for i = 1:$0\n    \nend'], ['شرط', 'if $0\n    \nend'], ['نمودار', 'plot($0);\ngrid on;'], ['چاپ', 'fprintf(\'$0\\n\');']],
    asm: [['بخش داده', 'section .data\n    $0 db 0'], ['بخش کد', 'section .text\n    global _start\n_start:\n    $0'], ['تابع', '$0:\n    push rbp\n    mov rbp, rsp\n    pop rbp\n    ret'], ['حلقه', '    mov rcx, $0\nloop_start:\n    dec rcx\n    jnz loop_start'], ['خروج', '    mov rax, 60\n    xor rdi, rdi\n    syscall']],
    pascal: [['برنامه', 'program $0;\nbegin\n  \nend.'], ['تابع', 'function $0: Integer;\nbegin\n  \nend;'], ['رویه', 'procedure $0;\nbegin\n  \nend;'], ['حلقه', 'for i := 1 to $0 do\nbegin\n  \nend;'], ['شرط', 'if $0 then\nbegin\n  \nend;'], ['چاپ', 'WriteLn($0);']],
    vb: [['ماژول', 'Module $0\n    Sub Main()\n        \n    End Sub\nEnd Module'], ['تابع', 'Function $0() As String\n    Return ""\nEnd Function'], ['رویه', 'Sub $0()\n    \nEnd Sub'], ['حلقه', 'For i As Integer = 1 To $0\n    \nNext'], ['شرط', 'If $0 Then\n    \nEnd If'], ['چاپ', 'Console.WriteLine($0)']],
    lua: [['تابع', 'local function $0()\n  \nend'], ['حلقه', 'for i = 1, $0 do\n  \nend'], ['شرط', 'if $0 then\n  \nend'], ['جدول', 'local $0 = {}'], ['چاپ', 'print($0)']],
    scala: [['شیء', 'object $0 {\n  \n}'], ['تابع', 'def $0(): Unit = {\n  \n}'], ['کلاس', 'case class $0(id: Int)'], ['تطبیق', '$0 match {\n  case _ => \n}'], ['حلقه', 'for (i <- 0 until $0) {\n  \n}'], ['چاپ', 'println($0)']],
    perl: [['تابع', 'sub $0 {\n    my () = @_;\n    \n}'], ['حلقه', 'foreach my $i ($0) {\n    \n}'], ['شرط', 'if ($0) {\n    \n}'], ['چاپ', 'print "$0\\n";'], ['آرایه', 'my @$0 = ();']],
    julia: [['تابع', 'function $0()\n    \nend'], ['حلقه', 'for i in 1:$0\n    \nend'], ['شرط', 'if $0\n    \nend'], ['ساختار', 'struct $0\n    \nend'], ['چاپ', 'println($0)']],
    fortran: [['برنامه', 'program $0\n  implicit none\n  \nend program'], ['تابع', 'function $0(x) result(y)\n  implicit none\n  \nend function'], ['حلقه', 'do i = 1, $0\n    \n  end do'], ['شرط', 'if ($0) then\n    \n  end if'], ['چاپ', 'print *, $0']],
    prolog: [['واقعیت', '$0(ali, reza).'], ['قاعده', '$0(X, Y) :-\n    parent(X, Y).'], ['پرسش', '?- $0(X).'], ['فهرست', 'member($0, [1, 2, 3]).'], ['چاپ', 'write($0), nl.']],
    lisp: [['تابع', '(defun $0 (x)\n  )'], ['متغیر', '(defvar *$0* nil)'], ['let', '(let (($0 1))\n  )'], ['شرط', '(if $0\n    t\n    nil)'], ['حلقه', '(dolist (x $0)\n  (print x))'], ['چاپ', '(print $0)']],
    groovy: [['تابع', 'def $0() {\n    \n}'], ['کلاس', 'class $0 {\n    \n}'], ['each', '$0.each {\n    \n}'], ['حلقه', 'for (i in 0..<$0) {\n    \n}'], ['چاپ', 'println $0']],
    objc: [['رابط', '@interface $0 : NSObject\n@end'], ['پیاده‌سازی', '@implementation $0\n@end'], ['متد', '- (void)$0 {\n    \n}'], ['حلقه', 'for (int i = 0; i < $0; i++) {\n    \n}'], ['چاپ', 'NSLog(@"$0");']]
  };
  function snippetsFor(id) {
    var L = lang(id);
    return (SNIPPETS[L.id] || []).map(function (s) { return {label: s[0], code: s[1]}; });
  }

  /* واژه‌های پیشنهادی کامل‌کردن خودکار: کلیدواژه‌ها + برچسب قطعه‌ها */
  var COMPLETE_CACHE = {};
  function completions(id) {
    var L = lang(id);
    if (COMPLETE_CACHE[L.id]) return COMPLETE_CACHE[L.id];
    var seen = {}, out = [];
    L.kw.split(/\s+/).forEach(function (w) {
      if (w && w.length > 1 && !seen[w]) { seen[w] = 1; out.push(w); }
    });
    out.sort(function (a, b) { return a.length - b.length || a.localeCompare(b); });
    COMPLETE_CACHE[L.id] = out;
    return out;
  }
  /* ---------------- رنگی‌کردن کد ---------------- */
  /* یک گذر روی متن: توضیح، رشته، عدد، کلیدواژه، تابع. خروجی HTML امن است
     چون هر تکه پیش از افزودن esc می‌شود. */
  function highlight(code, langId) {
    var L = lang(langId);
    var src = String(code == null ? '' : code);
    var kw = {};
    L.kw.split(/\s+/).forEach(function (w) { if (w) kw[L.id === 'sql' ? w.toLowerCase() : w] = 1; });
    var out = '', i = 0, n = src.length;
    function push(cls, text) { out += cls ? '<span class="cx-' + cls + '">' + esc(text) + '</span>' : esc(text); }
    var isWord = function (c) { return /[A-Za-z0-9_$\u00C0-\u024F]/.test(c || ''); };
    while (i < n) {
      var c = src[i], two = src.substr(i, 2);
      /* توضیح بلوکی */
      if (L.block && two === L.block[0]) {
        var end = src.indexOf(L.block[1], i + 2);
        end = end < 0 ? n : end + L.block[1].length;
        push('cm', src.slice(i, end)); i = end; continue;
      }
      /* توضیح خطی */
      if (L.line && src.substr(i, L.line.length) === L.line) {
        var nl = src.indexOf('\n', i); if (nl < 0) nl = n;
        push('cm', src.slice(i, nl)); i = nl; continue;
      }
      /* رشته‌ها */
      if (c === '"' || c === "'" || c === '`') {
        var j = i + 1;
        while (j < n) {
          if (src[j] === '\\') { j += 2; continue; }
          if (src[j] === c) { j++; break; }
          if (src[j] === '\n' && c !== '`') break;
          j++;
        }
        push('st', src.slice(i, j)); i = j; continue;
      }
      /* عدد */
      if (/[0-9]/.test(c) && !isWord(src[i - 1])) {
        var m = /^(0[xXbBoO][0-9a-fA-F_]+|[0-9][0-9_]*(\.[0-9_]+)?([eE][+-]?[0-9]+)?)/.exec(src.slice(i));
        if (m) { push('nu', m[0]); i += m[0].length; continue; }
      }
      /* واژه: کلیدواژه یا فراخوانی تابع */
      if (isWord(c) && !isWord(src[i - 1])) {
        var w = /^[\w$\u00C0-\u024F]+/.exec(src.slice(i))[0];
        var probe = L.id === 'sql' ? w.toLowerCase() : w;
        if (kw[probe]) push('kw', w);
        else if (/^\s*\(/.test(src.slice(i + w.length))) push('fn', w);
        else if (/^[A-Z]/.test(w)) push('ty', w);
        else push('', w);
        i += w.length; continue;
      }
      /* پیش‌پردازنده و متغیرهای زبان‌هایی مثل پی‌اچ‌پی */
      if ((c === '#' && L.id !== 'python' && L.id !== 'r') || c === '$') {
        var v = /^[#$][\w]+/.exec(src.slice(i));
        if (v) { push('pp', v[0]); i += v[0].length; continue; }
      }
      if (/[{}()[\];,.]/.test(c)) { push('pn', c); i++; continue; }
      if (/[+\-*/%=<>!&|^~?:]/.test(c)) { push('op', c); i++; continue; }
      push('', c); i++;
    }
    return out;
  }

  /* ---------------- بلوک نمایش (فقط‌خواندنی) ---------------- */
  function blockHtml(code, langId) {
    var L = lang(langId), src = String(code == null ? '' : code).replace(/\s+$/, '');
    var lines = src.split('\n');
    var nums = lines.map(function (_, i) { return i + 1; }).join('\n');
    return '<div class="cx-block" dir="ltr" data-lang="' + esc(L.id) + '">'
      + '<div class="cx-bar"><span class="cx-badge" style="--cx:' + L.color + '">' + esc(L.short) + '</span>'
      + '<span class="cx-name">' + esc(L.name) + '</span><span class="cx-grow"></span>'
      + '<span class="cx-meta">' + esc(String(lines.length)) + ' lines</span>'
      + '<button type="button" class="cx-copy" data-cx-copy="1">کپی</button></div>'
      + '<div class="cx-body"><pre class="cx-num" aria-hidden="true">' + esc(nums) + '</pre>'
      + '<pre class="cx-code"><code>' + highlight(src, L.id) + '</code></pre></div></div>';
  }

  /* ---------------- قالب ذخیره‌سازی ---------------- */
  var FENCE = /```([A-Za-z0-9_+#.\-]*)[ \t]*\r?\n([\s\S]*?)```/g;
  function fence(code, langId) {
    var L = lang(langId), body = String(code == null ? '' : code).replace(/\s+$/, '');
    /* اگر خود کد ``` داشته باشد، بلوک را نمی‌شکنیم؛ نویسهٔ کنترل را جایگزین می‌کنیم */
    body = body.replace(/```/g, '\u02CB\u02CB\u02CB');
    return '```' + L.id + '\n' + body + '\n```';
  }
  function parseFence(text) {
    var m = /```([A-Za-z0-9_+#.\-]*)[ \t]*\r?\n([\s\S]*?)```/.exec(String(text || ''));
    if (!m) return null;
    return {lang: lang(m[1]).id, code: m[2].replace(/\u02CB\u02CB\u02CB/g, '```').replace(/\s+$/, '')};
  }
  function hasFence(text) { return /```[A-Za-z0-9_+#.\-]*[ \t]*\r?\n/.test(String(text || '')); }
  /* متن را به تکه‌های متن/کد می‌شکند تا هر بخش با رندر درست خودش نشان داده شود */
  function split(text) {
    var src = String(text == null ? '' : text), parts = [], last = 0, m;
    FENCE.lastIndex = 0;
    while ((m = FENCE.exec(src))) {
      if (m.index > last) parts.push({code: false, text: src.slice(last, m.index)});
      parts.push({code: true, lang: lang(m[1]).id, text: m[2].replace(/\u02CB\u02CB\u02CB/g, '```')});
      last = m.index + m[0].length;
    }
    if (last < src.length) parts.push({code: false, text: src.slice(last)});
    return parts;
  }
  function html(text) {
    return split(text).map(function (p) {
      return p.code ? blockHtml(p.text, p.lang) : '<span class="cx-text">' + esc(p.text).replace(/\n/g, '<br>') + '</span>';
    }).join('');
  }
  /* کپی با یک کلیک روی هر بلوک کد در سایت */
  document.addEventListener('click', function (e) {
    var b = e.target && e.target.closest ? e.target.closest('[data-cx-copy]') : null;
    if (!b) return;
    e.preventDefault(); e.stopPropagation();
    var blk = b.closest('.cx-block'), pre = blk && blk.querySelector('.cx-code');
    var txt = pre ? pre.textContent : '';
    if (!txt) return;
    var done = function () { b.textContent = 'کپی شد'; setTimeout(function () { b.textContent = 'کپی'; }, 1400); };
    if (navigator.clipboard && navigator.clipboard.writeText) navigator.clipboard.writeText(txt).then(done, function () { toast('کپی نشد.', 'err'); });
    else {
      try { var ta = el('textarea', {style: 'position:fixed;opacity:0'}); ta.value = txt; document.body.appendChild(ta); ta.select(); document.execCommand('copy'); ta.remove(); done(); }
      catch (x) { toast('کپی نشد.', 'err'); }
    }
  });

  /* ---------------- انتخاب زبان ---------------- */
  function pickLanguage(onPick, opts) {
    opts = opts || {};
    var bg = el('div', {class: 'modal-bg'});
    function close() { bg.remove(); }
    bg._onCancel = close;
    var grid = el('div', {class: 'cx-grid'}, LANGS.map(function (L) {
      return el('button', {type: 'button', class: 'cx-tile', style: '--cx:' + L.color, onclick: function () { close(); onPick(L.id); }}, [
        el('span', {class: 'cx-tile-badge', text: L.short}),
        el('span', {class: 'cx-tile-name', text: L.name}),
        el('span', {class: 'cx-tile-ext', text: '.' + L.ext})
      ]);
    }));
    var m = el('div', {class: 'modal wide cx-modal'}, [
      el('button', {class: 'x', text: '✕', onclick: close}),
      el('h2', {text: opts.title || 'زبان برنامه‌نویسی را انتخاب کنید'}),
      el('p', {class: 'muted', text: opts.hint || 'محیط نوشتن کد با رنگی‌شدن دستورها و شمارهٔ خط باز می‌شود. کد اجرا نمی‌شود؛ فقط نوشته و ذخیره می‌شود.'}),
      grid
    ]);
    bg.appendChild(m);
    bg.addEventListener('click', function (e) { if (e.target === bg) close(); });
    document.body.appendChild(bg);
    return close;
  }

  /* ---------------- ویرایشگر ---------------- */
  function open(o) {
    o = o || {};
    var L = lang(o.lang);
    var readOnly = !!o.readOnly;
    var startCode = o.code != null && String(o.code) !== '' ? String(o.code) : (o.starter != null ? String(o.starter) : (o.blank ? '' : L.sample));
    var bg = el('div', {class: 'modal-bg cx-editor-bg'});
    var dirty = false, closed = false;

    var gutter = el('pre', {class: 'cx-num', 'aria-hidden': 'true'});
    var pre = el('pre', {class: 'cx-code cx-hl', 'aria-hidden': 'true'});
    var codeEl = el('code');
    pre.appendChild(codeEl);
    var ta = el('textarea', {class: 'cx-ta', spellcheck: 'false', autocapitalize: 'off', autocomplete: 'off',
      autocorrect: 'off', wrap: 'off', dir: 'ltr', maxlength: String(MAX)});
    ta.value = startCode;
    if (readOnly) ta.setAttribute('readonly', 'readonly');
    var pane = el('div', {class: 'cx-pane'}, [gutter, pre, ta]);

    /* V349 — نوار علائم، دکمه‌های آمادهٔ زبان، و فهرست کامل‌کردن خودکار */
    var keysRow = el('div', {class: 'cx-keys'});
    var snipRow = el('div', {class: 'cx-snips'});
    var acBox = el('div', {class: 'cx-ac', style: 'display:none'});
    var acItems = [], acIndex = -1, acOpen = false;

    function insertText(text) {
      var s = ta.selectionStart, t = ta.selectionEnd, v = ta.value;
      var at = text.indexOf('$0');
      var clean = at >= 0 ? text.slice(0, at) + text.slice(at + 2) : text;
      ta.value = v.slice(0, s) + clean + v.slice(t);
      ta.selectionStart = ta.selectionEnd = s + (at >= 0 ? at : clean.length);
      dirty = true; acHide(); paint();
      try { ta.focus(); } catch (x) {}
    }
    function buildRows() {
      keysRow.innerHTML = ''; snipRow.innerHTML = '';
      if (readOnly) { keysRow.style.display = 'none'; snipRow.style.display = 'none'; return; }
      symbolsFor(L.id).forEach(function (sym) {
        keysRow.appendChild(el('button', {type: 'button', class: 'cx-key', text: sym, title: 'درج ' + sym,
          onmousedown: function (e) { e.preventDefault(); }, onclick: function () { insertText(sym); }}));
      });
      snippetsFor(L.id).forEach(function (sn) {
        snipRow.appendChild(el('button', {type: 'button', class: 'btn light sm cx-snip', text: sn.label,
          onmousedown: function (e) { e.preventDefault(); }, onclick: function () { insertText(sn.code); }}));
      });
    }
    function acHide() { acOpen = false; acItems = []; acIndex = -1; acBox.style.display = 'none'; acBox.innerHTML = ''; }
    function acMark() {
      Array.prototype.forEach.call(acBox.children, function (c, i) { c.className = 'cx-ac-item' + (i === acIndex ? ' on' : ''); });
    }
    function acAccept(i) {
      if (!acOpen) return;
      var w = acItems[i == null ? acIndex : i];
      if (!w) return acHide();
      var v = ta.value, s = ta.selectionStart;
      var m = /[A-Za-z_][A-Za-z0-9_]*$/.exec(v.slice(0, s));
      if (!m) return acHide();
      /* بزرگی/کوچی حروفِ نوشتهٔ کاربر حفظ می‌شود (مثلاً SELECT در SQL) */
      var typed = m[0];
      if (typed === typed.toUpperCase() && typed.length > 1) w = w.toUpperCase();
      var start = s - typed.length;
      ta.value = v.slice(0, start) + w + v.slice(s);
      ta.selectionStart = ta.selectionEnd = start + w.length;
      dirty = true; acHide(); paint();
      try { ta.focus(); } catch (x) {}
    }
    function acShow() {
      if (readOnly) return;
      var v = ta.value, s = ta.selectionStart;
      if (s !== ta.selectionEnd) return acHide();
      var m = /[A-Za-z_][A-Za-z0-9_]*$/.exec(v.slice(0, s));
      if (!m || m[0].length < 2) return acHide();
      var word = m[0], lower = word.toLowerCase();
      var list = completions(L.id).filter(function (w) {
        return w.toLowerCase().indexOf(lower) === 0 && w.toLowerCase() !== lower;
      }).slice(0, 8);
      if (!list.length) return acHide();
      acItems = list; acIndex = 0; acOpen = true; acBox.innerHTML = '';
      list.forEach(function (w, i) {
        acBox.appendChild(el('button', {type: 'button', class: 'cx-ac-item' + (i === 0 ? ' on' : ''), text: w,
          onmousedown: function (e) { e.preventDefault(); }, onclick: function () { acAccept(i); }}));
      });
      acBox.style.display = '';
    }

    var badge = el('span', {class: 'cx-badge', text: L.short});
    var nameEl = el('span', {class: 'cx-name', text: L.name});
    var pos = el('span', {class: 'cx-meta', text: ''});
    var langBtn = el('button', {type: 'button', class: 'btn light sm', text: 'تغییر زبان'});
    var bar = el('div', {class: 'cx-bar'}, [badge, nameEl, el('span', {class: 'cx-grow'}), pos, langBtn]);

    function setLang(id) {
      L = lang(id);
      badge.textContent = L.short; badge.style.setProperty('--cx', L.color);
      nameEl.textContent = L.name;
      bar.style.setProperty('--cx', L.color);
      buildRows(); acHide();
      paint();
    }
    function paint() {
      var v = ta.value;
      codeEl.innerHTML = highlight(v, L.id) + '\n';
      var count = v.split('\n').length;
      var nums = [];
      for (var i = 1; i <= count; i++) nums.push(i);
      gutter.textContent = nums.join('\n');
      var upto = v.slice(0, ta.selectionStart || 0).split('\n');
      pos.textContent = 'Ln ' + upto.length + ', Col ' + (upto[upto.length - 1].length + 1) + ' · ' + v.length + '/' + MAX;
      sync();
    }
    function sync() { pre.scrollTop = ta.scrollTop; pre.scrollLeft = ta.scrollLeft; gutter.scrollTop = ta.scrollTop; }

    ta.addEventListener('input', function () { dirty = true; paint(); acShow(); });
    ta.addEventListener('scroll', sync);
    ta.addEventListener('click', function () { paint(); acHide(); });
    ta.addEventListener('blur', function () { setTimeout(acHide, 120); });
    ta.addEventListener('keyup', function (e) { if (e.key && e.key.indexOf('Arrow') === 0) paint(); });
    ta.addEventListener('keydown', function (e) {
      if (readOnly) return;
      /* V349 — وقتی فهرست پیشنهاد باز است، کلیدها آن را کنترل می‌کنند */
      if (acOpen) {
        if (e.key === 'ArrowDown') { e.preventDefault(); acIndex = (acIndex + 1) % acItems.length; acMark(); return; }
        if (e.key === 'ArrowUp') { e.preventDefault(); acIndex = (acIndex - 1 + acItems.length) % acItems.length; acMark(); return; }
        if (e.key === 'Tab' || e.key === 'Enter') { e.preventDefault(); acAccept(); return; }
        if (e.key === 'Escape') { e.preventDefault(); acHide(); return; }
      }
      var pad = new Array(L.indent + 1).join(' ');
      var s = ta.selectionStart, t = ta.selectionEnd, v = ta.value;
      /* Tab / Shift+Tab: تورفتگی یک یا چند خط */
      if (e.key === 'Tab') {
        e.preventDefault();
        var lineStart = v.lastIndexOf('\n', s - 1) + 1;
        if (e.shiftKey) {
          var block = v.slice(lineStart, t);
          var out = block.split('\n').map(function (ln) { return ln.replace(new RegExp('^ {1,' + L.indent + '}'), ''); }).join('\n');
          ta.value = v.slice(0, lineStart) + out + v.slice(t);
          ta.selectionStart = Math.max(lineStart, s - (block.length - out.length > 0 ? Math.min(L.indent, block.length - out.length) : 0));
          ta.selectionEnd = ta.selectionStart + out.length;
        } else if (s !== t && v.slice(s, t).indexOf('\n') >= 0) {
          var seg = v.slice(lineStart, t);
          var indented = seg.split('\n').map(function (ln) { return pad + ln; }).join('\n');
          ta.value = v.slice(0, lineStart) + indented + v.slice(t);
          ta.selectionStart = s + L.indent; ta.selectionEnd = t + (indented.length - seg.length);
        } else {
          ta.value = v.slice(0, s) + pad + v.slice(t);
          ta.selectionStart = ta.selectionEnd = s + L.indent;
        }
        dirty = true; paint(); return;
      }
      /* Enter: تورفتگی خط قبل حفظ می‌شود؛ بعد از { یا : یک پله بیشتر */
      if (e.key === 'Enter') {
        e.preventDefault();
        var ls = v.lastIndexOf('\n', s - 1) + 1;
        var cur = v.slice(ls, s);
        var lead = (/^[ \t]*/.exec(cur) || [''])[0];
        var extra = /[{:(\[]\s*$/.test(cur) ? pad : '';
        var closing = /^[ \t]*[})\]]/.test(v.slice(t)) && extra;
        var ins = '\n' + lead + extra + (closing ? '\n' + lead : '');
        ta.value = v.slice(0, s) + ins + v.slice(t);
        ta.selectionStart = ta.selectionEnd = s + 1 + lead.length + extra.length;
        dirty = true; paint(); return;
      }
      /* بستن خودکار پرانتز و کوتیشن */
      var pairs = {'(': ')', '[': ']', '{': '}'};
      if (pairs[e.key] && s === t) {
        e.preventDefault();
        ta.value = v.slice(0, s) + e.key + pairs[e.key] + v.slice(t);
        ta.selectionStart = ta.selectionEnd = s + 1;
        dirty = true; paint(); return;
      }
      if ((e.ctrlKey || e.metaKey) && (e.key === 's' || e.key === 'S')) { e.preventDefault(); save(); return; }
    });

    function close() { if (closed) return; closed = true; bg.remove(); if (o.onCancel) o.onCancel(); }
    function save() {
      if (closed) return;
      var v = ta.value;
      if (v.length > MAX) return toast('کد بلندتر از حد مجاز است.', 'err');
      closed = true; bg.remove();
      if (o.onDone) o.onDone({lang: L.id, code: v});
    }
    function askClose() {
      if (!dirty || readOnly) return close();
      if (S.confirmDlg) S.confirmDlg('بستن ویرایشگر', 'تغییرها ذخیره نشده‌اند. بسته شود؟', 'بستن').then(function (ok) { if (ok) close(); });
      else close();
    }
    bg._onCancel = askClose;
    bg.addEventListener('click', function (e) { if (e.target === bg) askClose(); });
    langBtn.addEventListener('click', function () {
      pickLanguage(function (id) {
        var wasSample = ta.value === L.sample;
        setLang(id);
        if (wasSample && !readOnly) { ta.value = L.sample; dirty = true; paint(); }
      }, {title: 'تغییر زبان', hint: 'اگر کد خالی یا نمونه باشد، نمونهٔ زبان تازه گذاشته می‌شود؛ کد نوشته‌شده دست‌نخورده می‌ماند.'});
    });

    var foot = el('div', {class: 'row cx-foot'}, [
      el('span', {class: 'muted cx-note', text: readOnly ? 'فقط نمایش — این کد پاسخ دانش‌آموز است.' : 'کد اجرا نمی‌شود؛ فقط نوشته و ذخیره می‌شود. تصحیح دستی است.'}),
      el('span', {class: 'grow'}),
      el('button', {type: 'button', class: 'btn light', text: readOnly ? 'بستن' : 'انصراف', onclick: askClose}),
      readOnly ? null : el('button', {type: 'button', class: 'btn', text: o.doneLabel || 'ذخیرهٔ کد', onclick: save})
    ]);
    var m = el('div', {class: 'modal wide cx-modal cx-editor'}, [
      el('button', {class: 'x', text: '✕', onclick: askClose}),
      el('h2', {text: o.title || 'محیط کدنویسی'}),
      bar, pane, acBox, keysRow, snipRow, foot
    ]);
    bg.appendChild(m);
    document.body.appendChild(bg);
    setLang(L.id);
    paint();
    if (!readOnly) setTimeout(function () { try { ta.focus(); ta.selectionStart = ta.selectionEnd = ta.value.length; } catch (x) {} }, 30);
    return {close: close, save: save};
  }

  /* باز کردن با انتخاب زبان (همان چیزی که کاربر خواست: لمس ابزار → فهرست زبان‌ها → محیط) */
  function openWithPicker(o) {
    o = o || {};
    if (o.lang) return open(o);
    return pickLanguage(function (id) { open(Object.assign({}, o, {lang: id})); }, o);
  }

  window.SiteCode = {
    LANGS: LANGS, lang: lang, highlight: highlight, blockHtml: blockHtml, html: html, split: split,
    fence: fence, parseFence: parseFence, hasFence: hasFence, open: open, openWithPicker: openWithPicker,
    pickLanguage: pickLanguage, symbolsFor: symbolsFor, snippetsFor: snippetsFor,
    completions: completions, MAX: MAX
  };
})();
