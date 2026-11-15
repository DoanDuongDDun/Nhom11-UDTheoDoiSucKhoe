exec(open('docs/uml/generate_diagrams.py',encoding='utf-8').read().split("sequence('01_")[0])
def draw(name,title,labels,events):
 xs=[280,650,1050,1480,1870];h=350+len(events)*85
 im=Image.new('RGB',(2100,h),'white');d=ImageDraw.Draw(im);txt(d,(50,30),title,34,True)
 for x,label in zip(xs,labels):
  d.rectangle((x-120,110,x+120,185),fill=PALE,outline=RED,width=2)
  for j,line in enumerate(label.split('|')):d.text((x,120+j*28),line,font=font(21,True),fill=INK,anchor='mt')
  dash(d,(x,190),(x,h-60))
 number=0
 for i,(a,b,t) in enumerate(events):
  y=250+i*85
  if a<0:
   d.polygon([(25,y-25),(210,y-25),(225,y-10),(225,y+25),(25,y+25)],fill=PALE,outline=RED);txt(d,(35,y-14),t,22,True);continue
  number+=1
  x,z=xs[a],xs[b];ret=t.startswith('return');t=str(number)+': '+t
  if x==z:
   d.line((x,y,x+60,y,x+60,y+20),fill=RED,width=2);arrow(d,(x+60,y+20),(x,y+20));txt(d,(x+8,y-30),t,20)
  else:
   arrow(d,(x,y),(z,y),ret);d.text(((x+z)/2,y-27),t,font=font(20),fill=INK,anchor='mt')
 txt(d,(50,h-40),'UML mức phân tích • Controller gộp ViewModel, Repository và xử lý nghiệp vụ; DAO truy cập SQLite.',20,fill=GRAY)
 im.save(OUT/name)
for body in [True,False]:
 key='co_the' if body else 'muc_tieu';title='CƠ THỂ' if body else 'MỤC TIÊU';obj='SoDo' if body else 'MucTieu'
 labels=['Người dùng',obj+'UI|«boundary»',obj+'Controller|«control»','Room / DAO|«data access»','CSDL|SQLite']
 ev=[(0,1,'chonChucNang'+obj+'()'),(1,2,'layDuLieu()'),(2,3,'observeDuLieu()'),(3,4,'selectDuLieu()'),(4,3,'returnDuLieu()'),(3,2,'returnLiveData()'),(2,1,'returnDanhSach()'),(1,1,'hienThiDanhSach()'),(0,1,'nhapThongTinVaLuu()'),(1,2,'xuLyLuu'+obj+'()'),(2,2,'kiemTraHopLe()'),(2,3,'insertHoacUpdate()'),(3,4,'ghiDuLieu()'),(4,3,'returnKetQuaLuu()'),(3,2,'returnDuLieuCapNhat()'),(2,2,'anhXaHinhThe2D()' if body else 'tinhPhanTramHoanThanh()'),(2,1,'returnKetQua()'),(1,1,'capNhatMoHinhVaLichSu()' if body else 'capNhatThanhTienDo()')]
 if body:ev +=[(0,1,'chonHaiGiaiDoan()'),(1,2,'soSanhHaiGiaiDoan()'),(2,1,'returnChenhLechVaHaiMoHinh()')]
 draw('01_trinh_tu_co_the.png' if body else '02_trinh_tu_muc_tieu.png','TRÌNH TỰ '+title+' — LUỒNG CƠ BẢN',labels,ev)
 ev=[(-1,-1,'Rẽ nhánh 1'),(0,1,'nhapThongTinKhongHopLe()'),(1,2,'kiemTraThongTin()'),(2,2,'kiemTraHopLe()'),(2,1,'returnLoiDuLieu()'),(1,1,'hienThiLoiDuLieu()'),(-1,-1,'Rẽ nhánh 2')]
 if body:ev +=[(0,1,'nhapSoDoTrungNgay()'),(1,2,'kiemTraTrungNgay()'),(2,1,'returnDanhSachTrungNgay()'),(1,1,'hoiGhiDeHayGiuCaHai()')]
 else:ev +=[(0,1,'datMucTieuCoThe()'),(1,2,'kiemTraSoDoBanDau()'),(2,1,'returnChuaCoSoDo()'),(1,1,'yeuCauNhapSoDoTruoc()')]
 ev +=[(-1,-1,'Rẽ nhánh 3'),(0,1,'huyVaQuayLai()'),(1,1,'hienThiManHinhTruoc()'),(-1,-1,'Rẽ nhánh 4'),(2,3,'luuDuLieu()'),(3,4,'ghiDuLieu()'),(4,3,'returnLoiGhiCSDL()'),(3,2,'returnLoiLuu()'),(2,1,'returnThongBaoLoi()'),(1,1,'hienThiLoiLuuDuLieu()')]
 draw('05_re_nhanh_co_the.png' if body else '06_re_nhanh_muc_tieu.png','TRÌNH TỰ '+title+' — CÁC TÌNH HUỐNG RẼ NHÁNH',labels,ev)
