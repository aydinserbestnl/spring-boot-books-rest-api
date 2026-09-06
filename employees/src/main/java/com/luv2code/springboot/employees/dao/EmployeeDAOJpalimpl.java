package com.luv2code.springboot.employees.dao;

import com.luv2code.springboot.employees.entity.Employee;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/*
Bu sınıfın veritabanı işlemleri yapan bir bileşen olduğunu Spring'e söyler. Spring uygulama açılırken bu etiketi taşıyan sınıfları otomatik bulur,
bir "bean" olarak hafızada tutar ve ihtiyaç duyulan yere kendisi enjekte eder — sen bu sınıfı elle new EmployeeDAOJpalimpl() diye oluşturmazsın.

EmployeeDAO interface'inin (sözleşmesinin) somut/gerçek kodla dolu hâli.
Interface'te sadece findAll()'un imzası vardı, burada gerçek implementasyonunu yazıyorsun.
 */
@Repository
public class EmployeeDAOJpalimpl implements EmployeeDAO {
    private final EntityManager entityManager;
/*
Buna constructor injection denir. Spring, sınıfı oluştururken elindeki hazır EntityManager'ı bu constructor'a otomatik olarak paslar,
sen de onu this.entityManager alanına atarsın. Bunu field'a direkt @Autowired yazmak yerine constructor üzerinden yapmanın avantajı:
bağımlılık dışarıdan net görünür ve test yazarken sahte (mock) bir EntityManager kolayca verilebilir.
 */
    @Autowired
    public EmployeeDAOJpalimpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<Employee> findAll() {
        //create  query
        //EntityManager'a "bana bir sorgu hazırla" diyorsun.
        /*
         "from Employee" — bu SQL değil, JPQL (Java Persistence Query Language).
         Dikkat et: tablo adı (employee) değil, Java sınıf adı (Employee) kullanılıyor.
         Bu, "tüm Employee kayıtlarını getir" anlamına geliyor;
         Hibernate bunu arka planda gerçek SQL'e (SELECT * FROM employee) çevirecek.
- Employee.class — sorgu sonucunun hangi tipte nesnelere dönüştürüleceğini söylüyor.
         */

        TypedQuery<Employee> theQuery = entityManager.createQuery("from Employee", Employee.class);

        //execute query and get result list
        List<Employee> employees = theQuery.getResultList();
        //return result list
        return employees;
    }

    @Override
    public Employee findById(long id) {
        return entityManager.find(Employee.class, id);
    }

    @Override
    @Transactional
    public Employee save(Employee employee) {
        return entityManager.merge(employee);
    }

    @Override
    @Transactional
    public void deleteById(long id) {
        Employee employee = entityManager.find(Employee.class, id);
        entityManager.remove(employee);
    }
}
/*
Sadece bunu senin yerine kim yapıyor, farkı orada.

Düz JDBC'de (elle) yaptığın iş:
ResultSet rs = statement.executeQuery("SELECT * FROM employee");
List<Employee> employees = new ArrayList<>();
while (rs.next()) {
    Employee emp = new Employee();
    emp.setId(rs.getLong("id"));
    emp.setFirstName(rs.getString("first_name"));
    emp.setLastName(rs.getString("last_name"));
    emp.setEmail(rs.getString("email"));
    employees.add(emp);
}
Burada satır satır ResultSet'i gezip, her sütunu (getString, getLong...) elle çekip, elle bir Employee nesnesine yerleştiriyorsun.
Hangi sütunun hangi field'a gideceğine sen karar veriyorsun, kod olarak yazıyorsun.

JPA/Hibernate'de ("from Employee" + getResultList()) olan şey:

Aynı işi Hibernate senin yerine yapıyor. getResultList() çağrıldığı an Hibernate arka planda:
1. Gerçek SQL'i çalıştırır (SELECT * FROM employee),
2. Dönen ResultSet'i satır satır gezer (senin yukarıda elle yaptığın while(rs.next()) döngüsünün aynısı — ama sen görmüyorsun, Hibernate'in içinde oluyor),
3. Her satır için rs.getString("first_name"), rs.getLong("id") gibi çağrıları kendisi yapar —
hangi sütunun hangi field'a gideceğini de senin Employee.java'daki @Column(name="first_name") gibi anotasyonlardan öğrenir,
4. Sonucu hazır bir Employee nesnesi olarak üretir ve listeye ekler.

Yani: @Column anotasyonları senin elle yazdığın rs.getString("first_name") satırlarının yerini alıyor —
sen o eşlemeyi (mapping) bir kere anotasyonla tanımlıyorsun, Hibernate her sorguda bunu otomatik uyguluyor.
Sonuç aynı (List<Employee>), ama ResultSet'i elle gezme işini sen değil, Hibernate senin yerine yapıyor.
 */
/*
yani bir entity daha olsaydi atiyorum, oraya from ... mi yazicaktik

Evet, aynen öyle — mantık her entity için tıpatıp aynı, sadece class adını değiştiriyorsun.

Örneğin projene bir Department entity'si eklesen (kendi @Entity, @Table(name="department") anotasyonlarıyla),
o entity için findAll() benzeri bir metot yazarken şöyle yazardın:

TypedQuery<Department> theQuery = entityManager.createQuery("from Department", Department.class);
List<Department> departments = theQuery.getResultList();

Dikkat edilecek noktalar:
- "from Department" içindeki isim, veritabanındaki tablo adı değil,
Java'daki entity sınıfının adı. Yani @Table(name="department") yazsan bile, JPQL'de yine Department (sınıf adı) yazarsın, department (tablo adı) değil.
- Employee.class yerine Department.class yazman gerekir — bu, sonucun hangi tipte nesnelere dönüştürüleceğini Java'ya söylüyor.
- TypedQuery<Department> ve List<Department> de aynı şekilde entity tipine göre değişir.

Yani genel kalıp şu: entityManager.createQuery("from <EntityClassAdi>", <EntityClassAdi>.class) —
her yeni entity için bu ikisini o entity'nin adıyla değiştirerek aynı deseni tekrar kullanırsın. EmployeeDAO/EmployeeDAOJpalimpl yapısına benzer şekilde,
 muhtemelen ayrı bir DepartmentDAO interface'i ve DepartmentDAOJpaImpl sınıfı da yazardın —
 her entity kendi DAO'sunu alır.
 */
/*
FROM bir tesadüf değil — JPQL (Java Persistence Query Language), bilinçli olarak SQL'e çok benzer görünecek şekilde tasarlanmış bir sorgu dili. Sebebi şu:

1. SQL'deki FROM ile aynı görevi görüyor: "hangi veri kümesinden okuyacağım?"

SQL'de:
SELECT * FROM employee
JPQL'de:
FROM Employee
İkisi de aynı soruyu cevaplıyor: "Bu sorgu hangi kaynaktan veri çekecek?" Sadece SQL'de kaynak bir tablo, JPQL'de kaynak bir entity (Java sınıfı).

2. Java/Hibernate ekibi bunu bilerek SQL'e benzetti

JPA'yı tasarlayanlar, SQL zaten milyonlarca geliştiricinin bildiği bir dil olduğu için, JPQL'i sıfırdan farklı bir sözdizimiyle icat etmek yerine
SQL'in SELECT ... FROM ... WHERE ... ORDER BY yapısını olduğu gibi kopyaladılar.
Böylece SQL bilen biri JPQL'i neredeyse hiç öğrenmeden okuyabiliyor — sadece "tablo yerine entity, sütun yerine field kullanıyorum" demeyi öğrenmesi yeterli.

3. Aslında yazdığın satır bir kısaltma

Senin yazdığın:
"from Employee"
Açık (uzun) hâli şudur:
"SELECT e FROM Employee e"
Burada e, Employee entity'sini temsil eden bir değişken (JPQL'de buna "alias" denir).
SELECT e kısmı "her e'yi (yani her Employee nesnesinin tamamını) getir" demek.
Sen bunu yazmasan bile, SELECT kısmı belirtilmezse JPQL varsayılan olarak "tüm entity'yi getir" anlar —
bu yüzden "from Employee" tek başına da geçerli ve çalışıyor.

Yani özetle: FROM, SQL'den bilinçli olarak alınmış bir anahtar kelime; görevi "sorgunun hangi entity/tablo üzerinden çalışacağını" belirtmek —
SQL'deki karşılığıyla birebir aynı mantık, sadece tablo yerine entity sınıfı üzerinde çalışıyor.
 */
/*
 Employee sınıfının kendisi hiçbir zaman veri tutmaz, bu doğru. O sadece bir kalıp/şablon (blueprint).
 from Employee yazdığında Hibernate, o sınıfın içinden veri okumuyor —
 sınıfı bir anahtar/referans olarak kullanıp gerçek veriye (tablodaki satırlara) ulaşıyor. Adım adım ne oluyor:

1. Uygulama açılırken (senin sorgunu çalıştırmadan çok önce)
Hibernate, projendeki tüm @Entity etiketli sınıfları tarar (bu senaryoda sadece Employee). Her biri için kafasında bir "eşleme tablosu" (mapping) oluşturur:
Employee sınıfı  --->  "employee" tablosu   (@Table(name="employee") sayesinde)
Employee.id       --->  employee.id          (@Column sayesinde)
Employee.firstName --->  employee.first_name (@Column sayesinde)
...
Bu eşleme, Hibernate'in hafızasında (metadata olarak) durur — henüz hiçbir veri çekilmedi, sadece "hangi sınıf hangi tabloya karşılık geliyor" bilgisi hazırlandı.

2. Sen "from Employee" yazdığında
Hibernate bunu okuyunca veriyi doğrudan sınıftan almaya çalışmıyor.
Bunun yerine kendi kendine soruyor: "Employee sınıfı hangi tabloya eşleniyordu?"
— 1. adımda hazırladığı eşleme tablosuna bakıp cevabı buluyor: "employee" tablosu.

3. Gerçek SQL'e çeviriyor
Bulduğu bu bilgiyle arka planda gerçek SQL üretiyor:
SELECT id, first_name, last_name, email FROM employee
İşte veri buradan, gerçek veritabanı tablosundan geliyor — sınıftan değil.

4. Veritabanından gelen satırları Employee nesnelerine dönüştürüyor
Her satır için yeni bir Employee nesnesi yaratıp, sütun değerlerini o nesnenin field'larına yerleştiriyor (yine aynı eşleme bilgisini kullanarak).

---

Yani from Employee'deki "Employee" kelimesi, veriyi kendi içinde barındıran bir kaynak değil
— "hangi tabloya bakmam gerektiğini bana söyleyen bir işaret/anahtar".
FROM kelimesi de tam bunu ifade ediyor: "sorgunun kaynağı budur" diyor, ama o kaynak aslında iki katmanlı —
önce entity (Java tarafı), sonra Hibernate onu gerçek tabloya (veritabanı tarafı) çeviriyor.
Sonuçta akan gerçek veri hep tablodan gelir,
sınıfın kendisi hep boş bir kalıp olarak kalır;
sorgu bittikten sonra o kalıptan doldurulmuş nesneler (instance'lar) üretilmiş olur.
 */
/*
 /*
        "from Employee" içindeki Employee, evet senin yazdığın Employee sınıfını (entity'yi) kastediyor —
        ama "field'larını getir" demek tam doğru değil. Daha doğrusu şöyle:

- Bu ifade "her bir Employee nesnesinin tamamını getir" demek.
Yani veritabanındaki employee tablosundaki her satır için,
o satırın tüm sütunlarını (id, first_name, last_name, email) okuyup, bunlardan tek bir bütün Employee nesnesi oluşturuyor —
field'ları tek tek ayrı ayrı getirmiyor, hepsini birden bir paket (obje) hâlinde getiriyor.

Örnekle düşünürsen: Veritabanında şöyle bir satır olsun:

┌─────┬────────────┬───────────┬────────────────┐
│ id  │ first_name │ last_name │     email      │
├─────┼────────────┼───────────┼────────────────┤
│ 1   │ Ahmet      │ Yılmaz    │ ahmet@mail.com │
└─────┴────────────┴───────────┴────────────────┘

"from Employee" sorgusu bu satırı okuyup şuna eşdeğer bir Java nesnesi üretir:
new Employee(1, "Ahmet", "Yılmaz", "ahmet@mail.com")
Tablo kaç satırdan oluşuyorsa, sonuçta o kadar Employee nesnesi üretilir ve List<Employee> içine konur.

SQL ile farkı da tam burada: Normal SQL'de SELECT * FROM employee yazsan, tablo adını (employee, küçük harf) kullanırsın ve sonuç ham satır/sütun verisidir.
JPQL'de ise from Employee yazarken Java sınıf adını (Employee, büyük harf E) kullanırsın ve sonuç hazır Java nesneleridir —
Hibernate senin yerine "hangi sütun hangi field'a gidiyor" eşlemesini (@Column anotasyonları sayesinde) otomatik yapıp nesneyi doldurur.

Yani özetle: "from Employee" = "tabloya karşılık gelen her satırı, o satırın tüm alanlarıyla birlikte bir Employee nesnesi olarak getir" demek, sadece belirli field'ları değil.
         */
