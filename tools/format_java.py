from pathlib import Path
import re
# Format generated Java without changing tokens; preserve strings and comments.
def format_java(s):
 tokens=re.findall(r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|//[^\n]*|/\*[\s\S]*?\*/|[^"\'/]+|.',s)
 out=[];line='';indent=0;paren=0
 def emit():
  nonlocal line
  if line.strip():out.append('    '*max(indent,0)+line.strip())
  line=''
 for token in tokens:
  if token.startswith(('"',"'",'/*','//')):
   if token.startswith('//'):emit();line=token;emit()
   elif token.startswith('/*'):emit();line=token;emit()
   else:line+=token
   continue
  for ch in token:
   if ch=='(':paren+=1
   elif ch==')':paren-=1
   if ch=='{':line+=' {';emit();indent+=1
   elif ch=='}':emit();indent-=1;line='}';emit()
   elif ch==';':line+=';';emit() if paren==0 else None
   elif ch.isspace():
    if line and not line.endswith(' '):line+=' '
   else:line+=ch
 emit();return '\n'.join(out)+'\n'
for root in ['app/src/main/java','app/src/test/java','app/src/androidTest/java']:
 for p in Path(root).rglob('*.java'):p.write_text(format_java(p.read_text(encoding='utf-8-sig')),encoding='utf-8')
