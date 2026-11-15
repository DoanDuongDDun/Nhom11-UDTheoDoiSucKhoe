from PIL import Image, ImageDraw, ImageFont
from pathlib import Path
import math
OUT=Path('docs/uml')
INK='#26354A'; RED='#C83959'; PALE='#FFFBE7'; GRAY='#6F7B8D'
def font(n,b=False):return ImageFont.truetype('C:/Windows/Fonts/'+('arialbd.ttf' if b else 'arial.ttf'),n)
def canvas(title,sub,h):
 im=Image.new('RGB',(1800,h),'white');d=ImageDraw.Draw(im)
 d.text((60,35),title,font=font(39,True),fill=INK);d.text((60,93),sub,font=font(23),fill=GRAY)
 return im,d
def txt(d,xy,s,n=25,b=False,fill=INK):d.text(xy,s,font=font(n,b),fill=fill)
def dash(d,a,b,fill=GRAY):
 x,y=a;xx,yy=b;dist=math.hypot(xx-x,yy-y)
 for i in range(0,int(dist),14):
  t=i/dist;u=min(i+7,dist)/dist;d.line((x+(xx-x)*t,y+(yy-y)*t,x+(xx-x)*u,y+(yy-y)*u),fill=fill,width=2)
def arrow(d,a,b,dotted=False):
 if dotted:dash(d,a,b,RED)
 else:d.line((*a,*b),fill=RED,width=2)
 angle=math.atan2(b[1]-a[1],b[0]-a[0]);p=[b,(b[0]-14*math.cos(angle-.45),b[1]-14*math.sin(angle-.45)),(b[0]-14*math.cos(angle+.45),b[1]-14*math.sin(angle+.45))];d.line([p[1],p[0],p[2]],fill=RED,width=2)
def sequence(name,title,participants,events,foot):
 h=350+len(events)*83+120;im,d=canvas(title,'Biểu đồ trình tự • UML mức phân tích • Vitality',h)
 xs=[135,570,1040,1530]
 for i,label in enumerate(participants):
  x=xs[i]
  if i==0:
   d.ellipse((x-12,147,x+12,171),outline=RED,width=2);d.line((x,171,x,207),fill=RED,width=2);d.line((x-22,185,x+22,185),fill=RED,width=2);d.line((x,207,x-20,230),fill=RED,width=2);d.line((x,207,x+20,230),fill=RED,width=2)
   txt(d,(x-75,239),label,22,True)
  else:
   d.rectangle((x-185,160,x+185,255),fill=PALE,outline=RED,width=2)
   for k,line in enumerate(label.split('\n')):d.text((x,178+k*31),line,font=font(24,True),fill=INK,anchor='mt')
  dash(d,(x,280),(x,h-120))
 for idx,event in enumerate(events):
  a,b,label,kind=event;y=320+idx*83
  if kind=='note':
   d.rectangle((290,y-22,1700,y+36),fill='#F1F5F9',outline='#D3DAE2');txt(d,(308,y-10),label,23,True);continue
  x,z=xs[a],xs[b]
  if a==b:
   d.line((x,y,x+75,y,x+75,y+27),fill=RED,width=2);arrow(d,(x+75,y+27),(x,y+27));txt(d,(x+12,y-33),label,22)
  else:
   d.rectangle((z-5,y-8,z+5,y+30),fill='white',outline=RED,width=2)
   arrow(d,(x,y),(z,y),kind=='return');d.text(((x+z)/2,y-29),label,font=font(22),fill=INK,anchor='mt')
 txt(d,(60,h-78),foot,22);txt(d,(60,h-45),'Nét liền: yêu cầu/xử lý • Nét đứt: dữ liệu hoặc kết quả trả về.',20,fill=GRAY)
 im.save(OUT/name)
sequence('01_trinh_tu_co_the.png','MÔ PHỎNG CƠ THỂ NGƯỜI', ['Người dùng','CoTheUI\n«boundary»','CoTheController\n«control»','SO_DO_CO_THE\n«entity / Room»'],[
(0,1,'1: chonChucNangCoThe()',''),(1,2,'2: layLichSuSoDo()',''),(2,3,'3: getDanhSachSoDo()',''),(3,2,'4: returnDanhSachSoDo()','return'),(2,1,'5: hienThiMoHinhVaLichSu()','return'),
(0,1,'6: nhapVaLuuSoDo()',''),(1,2,'7: xuLyLuuSoDo()',''),(2,2,'8: kiemTraSoDoHopLe()',''),(0,0,'alt [Không hợp lệ]: báo lỗi tại ô nhập; người dùng sửa và lưu lại.','note'),(2,3,'9: luuSoDo()',''),(3,2,'10: returnKetQuaLuu()','return'),(2,2,'11: anhXaHinhThe2D()',''),(2,1,'12: capNhatMoHinhVaLichSu()','return'),(0,1,'13: chonHaiGiaiDoan()',''),(1,2,'14: soSanhHaiGiaiDoan()',''),(2,2,'15: tinhChenhLechSoDo()',''),(2,1,'16: hienThiSoSanh()','return')], 'Bước 13–16 là tùy chọn khi đã có ít nhất hai giai đoạn. Hình thể chỉ mang tính minh họa.')
sequence('02_trinh_tu_muc_tieu.png','ĐẶT VÀ THEO DÕI MỤC TIÊU',['Người dùng','MucTieuUI\n«boundary»','MucTieuController\n«control»','Dữ liệu Room\nMục tiêu / nhật ký / số đo'],[
(0,1,'1: chonChucNangMucTieu()',''),(1,2,'2: layMucTieuVaTienDo()',''),(2,3,'3: getMucTieuVaDuLieuThucTe()',''),(3,2,'4: returnDuLieuThucTe()','return'),(2,1,'5: hienThiMucTieuTheoNhom()','return'),(0,1,'6: nhapVaLuuMucTieu()',''),(1,2,'7: xuLyLuuMucTieu()',''),(2,2,'8: kiemTraVaLayMocBanDau()',''),(0,0,'alt [Thiếu/sai dữ liệu hoặc chưa có số đo cơ thể]: thông báo và yêu cầu bổ sung.','note'),(2,3,'9: themHoacCapNhatMucTieu()',''),(3,2,'10: returnKetQuaLuu()','return'),(2,1,'11: hienThiMucTieuDaLuu()','return'),(0,0,'opt [Ghi nhận thực tế]: người dùng nhập nhật ký hoặc số đo ở chức năng liên quan.','note'),(0,3,'12: ghiNhanNhatKyHoacSoDo()',''),(3,2,'13: thongBaoDuLieuThayDoi()','return'),(2,2,'14: tinhTienDoTheoNgay()',''),(2,1,'15: hienThiThanhPhanTram()','return')], 'Bước 12 rút gọn luồng của chức năng khác. Ví dụ nước: 1.000 / 2.000 ml = 50%.')
def box(d,x,y,w,title,attrs,methods):
 head=56;ah=max(48,len(attrs)*35+22);mh=len(methods)*35+22;h=head+ah+mh
 d.rectangle((x,y,x+w,y+h),fill=PALE,outline=RED,width=2)
 d.text((x+w/2,y+15),title,font=font(27,True),fill=INK,anchor='mt')
 d.line((x,y+head,x+w,y+head),fill=RED,width=2);d.line((x,y+head+ah,x+w,y+head+ah),fill=RED,width=2)
 for i,t in enumerate(attrs):txt(d,(x+16,y+head+10+i*35),t,23)
 for i,t in enumerate(methods):txt(d,(x+16,y+head+ah+10+i*35),t,23)
 return h
def relation(d,a,b,label,left='1',right='1',dotted=False):
 arrow(d,a,b,dotted);txt(d,(a[0]+8,a[1]-30),left,22);txt(d,(b[0]-40,b[1]-30),right,22)
 d.text(((a[0]+b[0])/2+(75 if a[0]==b[0] else 0),(a[1]+b[1])/2-30),label,font=font(22),fill=INK,anchor='mt')
im,d=canvas('MÔ PHỎNG CƠ THỂ NGƯỜI','Biểu đồ lớp • UML mức phân tích • Thành phần cốt lõi',1380)
box(d,60,190,650,'CoTheUI  «boundary»',[],['+ hienThiMoHinhVaLichSu()','+ nhapVaLuuSoDo()','+ chonHaiGiaiDoan()','+ hienThiSoSanh()','+ hienThiLoi()'])
box(d,1010,190,730,'CoTheController  «control»',[],['+ layLichSuSoDo()','+ kiemTraSoDoHopLe()','+ luuSoDo()','+ anhXaHinhThe2D()','+ soSanhHaiGiaiDoan()'])
relation(d,(710,310),(1010,310),'sử dụng')
box(d,1010,770,730,'SO_DO_CO_THE  «entity»',['+ id: long','+ measuredAt: long','+ heightCm, weightKg: double','+ chestCm, waistCm, hipCm: double','+ upperArmCm, thighCm, calfCm: Double','+ bodyTemplate, rendererVersion: int'],[])
relation(d,(1375,491),(1375,770),'quản lý','1','0..*')
for i,t in enumerate(['Ghi chú ánh xạ sang mã nguồn:','• UI: BodyFragment, MeasurementEditorFragment,','  ComparisonFragment và các Custom View.','• Điều khiển: VitalityViewModel, BodyRepository,','  Validation, BodyShapeMapper, HealthMath.','• Dữ liệu: BodyMeasurement; truy cập qua','  BodyMeasurementDao và VitalityDatabase.','• Canvas / Path / Paint vẽ hình cơ thể 2D.']):txt(d,(65,800+i*43),t,23,i==0)
txt(d,(60,1298),'Các lớp boundary/control là nhóm trách nhiệm phân tích, không phải tên lớp Java mới.',23,fill=GRAY)
im.save(OUT/'03_lop_co_the.png')
im,d=canvas('ĐẶT VÀ THEO DÕI MỤC TIÊU','Biểu đồ lớp • UML mức phân tích • Thành phần cốt lõi',1550)
box(d,60,180,650,'MucTieuUI  «boundary»',[],['+ hienThiMucTieuTheoNhom()','+ nhapVaLuuMucTieu()','+ chonNgayXemTienDo()','+ hienThiThanhPhanTram()','+ xoaMucTieu()'])
box(d,1010,180,730,'MucTieuController  «control»',[],['+ kiemTraMucTieuHopLe()','+ themHoacCapNhatMucTieu()','+ layDuLieuThucTeTheoNgay()','+ tinhTienDoTungMucTieu()','+ tinhTienDoChungTrongNgay()','+ xoaMucTieu()'])
relation(d,(710,300),(1010,300),'sử dụng')
box(d,1010,790,730,'MUC_TIEU  «entity»',['+ id: long','+ kind: String','+ title: String','+ keyword: String','+ target: double','+ baseline: double','+ createdAt: long'],[])
relation(d,(1375,516),(1375,790),'quản lý','1','0..*')
box(d,60,790,650,'DỮ LIỆU THỰC TẾ  «nguồn đọc»',['JournalEntry: kind, amount, startedAt,','endedAt, title, note, awakeMinutes','BodyMeasurement: measuredAt,','weightKg, waistCm'],[])
arrow(d,(1100,516),(385,790),True);txt(d,(570,675),'đọc để tính tiến độ',23)
for i,t in enumerate(['Ánh xạ mã nguồn: UI → GoalsFragment, GoalEditorFragment; điều khiển → VitalityViewModel,', 'GoalRepository, GoalProgress. MUC_TIEU → Goal; lưu bằng GoalDao / Room.', 'Không có khóa ngoại trực tiếp giữa Goal và JournalEntry/BodyMeasurement.', 'Nhật ký được lọc theo loại, ngày, từ khóa; cơ thể dùng số đo mới nhất và mốc ban đầu.', 'Boundary/control là nhóm trách nhiệm phân tích; nguồn đọc không phải một bảng mới.']):txt(d,(60,1250+i*45),t,23,fill=GRAY)
im.save(OUT/'04_lop_muc_tieu.png')
print('Generated 4 PNG diagrams in',OUT.resolve())
