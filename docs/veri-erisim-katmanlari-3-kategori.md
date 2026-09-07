# Veri Erişim Katmanları: 3 Kategori (ve bir de "var ama kullanmadığımız" 4.sü)

Bu doküman şu soruya cevap veriyor: bu repodaki projeler (`01-books`, `02-books`,
`employees`, `employee-security`, `employee-jdbc-security`, yeni başlanan `todos`)
ve `Desktop/project-2/BuroBOVProject-review` (okul projesi) arasında, veriyi
nasıl sakladıkları açısından kaç farklı yaklaşım var, bunların gerçek dünyadaki
adları ne, ve birbirlerinden hangi somut şeylerle (dependency, annotasyon, DB
bağlantısı) ayrılıyorlar.

İncelenen projelerin pom.xml ve kaynak kodlarına doğrudan bakılarak yazıldı —
varsayım değil, gözlem.

## Özet tablo

| Kategori | Gerçek dünyadaki adı | Bu repoda / okulda hangi projeler |
|---|---|---|
| 1 | In-memory (kalıcı depolama yok) | `01-books`, `02-books` |
| 2 | Spring JDBC (düz JDBC, `JdbcTemplate`) | `employee-jdbc-security`, `BuroBOVProject-review` |
| 3 | Spring Data JPA (Hibernate üzerinden) | `employees`, `employee-security`, `todos` (yeni başladığın) |
| (kullanılmayan 4.) | Spring Data JDBC | Hiçbirinde yok — ama karıştırılmaya çok müsait, aşağıda açıklandı |

---

## Kategori 1: In-memory — `01-books`, `02-books`

### Kanıt

`02-books/src/main/java/com/love2code/books/controller/BookController.java`:
```java
private final List<Book> books = new ArrayList<>();
```

`pom.xml`'de hiçbir veritabanı/persistence dependency'si yok — sadece:
```xml
spring-boot-starter-webmvc
springdoc-openapi-starter-webmvc-ui
spring-boot-starter-validation
```

### Ne demek

Kitaplar hiçbir veritabanına yazılmıyor — doğrudan controller'ın içinde bir
Java `ArrayList`'te, **RAM'de** tutuluyor. Uygulama kapanınca (ya da restart
olunca) tüm veri sıfırlanıyor. DAO yok, Repository yok, SQL yok.

### Gerçek dünyada bu neye denir

Resmi bir "teknoloji adı" yok çünkü bu bir framework/kütüphane değil, sadece
"veriyi bir koleksiyonda tutmak". Yaygın adlandırmalar:
- **"In-memory storage"** / **"in-memory repository"**
- Genellikle sadece **prototip, demo, ilk öğretim adımı** ya da **unit
  test'lerde sahte (fake/mock) bir repository** olarak kullanılır.
- Gerçek bir production sisteminde tek başına (kalıcılık sağlamadan) neredeyse
  hiç kullanılmaz — uygulama yeniden başlayınca veri kaybolur.

---

## Kategori 2: Spring JDBC — `employee-jdbc-security`, `BuroBOVProject-review`

### Kanıt

`employee-jdbc-security/pom.xml`:
```xml
<artifactId>spring-boot-starter-jdbc</artifactId>
```

`BuroBOVProject-review/pom.xml`:
```xml
<artifactId>spring-boot-starter-jdbc</artifactId>
<artifactId>mysql-connector-j</artifactId>
```

Domain/entity sınıfı **hiçbir annotasyon taşımıyor** — düz bir POJO ya da
record. `BuroBOVProject-review`'daki `Gebruiker.java`:
```java
public class Gebruiker {
    private Long id;
    private String initials;
    // ... sadece field + getter/setter, hic @Entity/@Table/@Column yok
}
```
`employee-jdbc-security`'deki `Employee.java`:
```java
public record Employee(long id, String firstName, String lastName, String email) {}
```

DAO katmanı elle SQL yazıyor, `JdbcTemplate` + `RowMapper` kullanıyor.
`BuroBOVProject-review/.../JdbcGebruikerDao.java`:
```java
@Repository
public class JdbcGebruikerDao implements GebruikerDao {
    private final JdbcTemplate jdbcTemplate;
    public List<Gebruiker> findAll() {
        return jdbcTemplate.query("SELECT * FROM Gebruiker", new GebruikerRowMapper());
    }
}
```
`employee-jdbc-security/.../EmployeeDAOJdbcImpl.java` da birebir aynı desende
(bkz. önceki mesajlardaki kod).

### Ne demek

- Tablo/kolon eşleşmesi **annotasyonla değil, senin yazdığın SQL metniyle**
  kuruluyor (`"SELECT * FROM Gebruiker"`, `rs.getString("first_name")`).
- Hiçbir "ORM" (object-relational mapper), hiçbir persistence context, hiçbir
  lazy-loading/dirty-checking mekanizması yok. Her `jdbcTemplate.query(...)`
  çağrısı, tam olarak yazdığın SQL'i, o an, doğrudan veritabanına gönderir.
- Domain sınıfı tamamen "salt" — Spring'in, Hibernate'in bu sınıftan hiç
  haberi yok, sadece senin DAO kodun bu sınıfı kullanıyor.

### Gerçek dünyada bu neye denir

**"Spring JDBC"** (bazen sadece "JDBC" ya da "raw/plain JDBC" da denir).
`spring-boot-starter-jdbc` dependency'si, Spring'in ham JDBC üzerine ince bir
kolaylık katmanı (`JdbcTemplate`) koyduğu modülün adı. Gerçek şirket
projelerinde tercih edilme sebepleri genelde: tam SQL kontrolü isteme, çok
karmaşık/performans-kritik sorgular, ORM'in "sihirli" davranışlarından
(N+1 query, lazy loading sürprizleri) kaçınma isteği.

**Dikkat — isim karışıklığı riski:** Bu, **"Spring Data JDBC"** ile aynı şey
DEĞİL (aşağıda ayrıca açıklandı). `spring-boot-starter-jdbc` ≠
`spring-boot-starter-data-jdbc`. İsimleri çok benzer ama farklı iki proje.

---

## Kategori 3: Spring Data JPA — `employees`, `employee-security`, `todos`

### Kanıt

`employees/pom.xml`:
```xml
<artifactId>spring-boot-starter-data-jpa</artifactId>
```
`todos/pom.xml` (yeni başladığın proje) da **aynı** dependency'yi kullanıyor:
```xml
<artifactId>spring-boot-starter-data-jpa</artifactId>
```
(Yani senin şüphelendiğin gibi "JPA data" diye ayrı bir 3. kategori değil —
`todos` da tam olarak `employees` ile aynı kategoride, sadece henüz kodu
yazılmamış, `Todo.java` şu an boş bir sınıf.)

Domain sınıfı annotasyonlu:
```java
@Entity
@Table(name = "employee")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;
    @Column(name = "first_name")
    private String firstName;
    ...
}
```

DAO katmanı yok — yerine bir arayüz, hiç implementasyon yazmadan:
```java
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    // hicbir kod yok, Spring Data JPA calisma zamaninda kendisi implemente ediyor
}
```

### Ne demek

- Tablo/kolon eşleşmesi annotasyonla **deklaratif** — SQL'i sen yazmıyorsun,
  Hibernate senin yerine üretiyor.
- `JpaRepository<Employee, Long>`'u extend etmek yeterli — `findAll()`,
  `findById()`, `save()`, `deleteById()` gibi metotlar **hiç kod yazmadan**
  hazır geliyor (Spring Data JPA, arayüzün adına/imzasına bakarak arka planda
  bir implementasyon üretiyor — buna "proxy" denir).
- Persistence context, lazy loading, dirty checking gibi Hibernate
  mekanizmaları devrede.

### Gerçek dünyada bu neye denir

Üç katmanlı bir isimlendirme var, sık sık birbirinin yerine kullanılıyor ama
teknik olarak farklı katmanlar:

1. **JPA** (Jakarta Persistence API) — sadece bir **spesifikasyon**
   (arayüzler/kurallar bütünü, `@Entity`, `@Id` gibi annotasyonlar dahil).
   Kendi başına çalışan bir kod değil, bir "sözleşme".
2. **Hibernate** — bu spesifikasyonu **gerçekleştiren** (implement eden) asıl
   motor. SQL'i fiilen üreten, DB'ye bağlanan kod budur.
3. **Spring Data JPA** — Hibernate'in (veya JPA'nın) üzerine, `JpaRepository`
   gibi hazır arayüzlerle "hiç DAO implementasyonu yazma" kolaylığı katan
   Spring modülü.

Konuşma dilinde insanlar genelde "JPA kullanıyoruz" ya da "Hibernate
kullanıyoruz" der, ikisi de kabaca doğrudur çünkü genelde üçü birlikte gelir.
En doğru/spesifik terim, bizim projelerde gördüğümüz senaryo için:
**"Spring Data JPA (Hibernate implementasyonuyla)"**.

---

## Ekosistemde var olan ama HİÇBİR projende kullanılmayan 4. seçenek: Spring Data JDBC

Senin "3 kategori mi var" sorunun arkasında muhtemelen bu isim karışıklığı
var, o yüzden ayrıca belirtiyorum: **Spring Data JDBC**, yukarıdaki
Kategori 2 (Spring JDBC) ile Kategori 3 (Spring Data JPA) arasında,
gerçekten var olan üçüncü bir Spring projesi — ama incelediğimiz hiçbir
projede kullanılmıyor.

Ne yapar: `JpaRepository`'ye benzer şekilde `CrudRepository`'yi extend edip
hiç implementasyon yazmadan `save()`/`findAll()` gibi metotlar elde edersin
(Kategori 3'e benzer kolaylık), AMA arkada Hibernate/JPA yok — her işlem
doğrudan, basit bir SQL ifadesiyle çalışır, lazy loading/persistence context
gibi "sihir" yok (Kategori 2'nin sadeliğine yakın). Yani "JPA'nın kolaylığı
+ JDBC'nin öngörülebilirliği" gibi bir orta yol. Dependency adı
`spring-boot-starter-data-jdbc` olurdu (dikkat: `starter-jdbc` değil,
`starter-data-jdbc` — tek kelime fark ama tamamen farklı bir modül).

---

## Her kategoriyi ayıran somut teknik farklar (tablo)

| | Kategori 1: In-memory | Kategori 2: Spring JDBC | Kategori 3: Spring Data JPA |
|---|---|---|---|
| **Maven dependency** | yok | `spring-boot-starter-jdbc` | `spring-boot-starter-data-jpa` |
| **Domain sınıfı annotasyonu** | yok | yok (düz POJO/record) | `@Entity`, `@Table`, `@Column`, `@Id` |
| **Tablo/kolon eşleşmesi** | yok (tablo yok) | elle, SQL metninde (`"first_name"`) | annotasyonla, deklaratif |
| **DAO/Repository kodu** | yok (direkt `List`) | elle yazılır (`JdbcTemplate`, `RowMapper`) | yazılmaz, arayüz yeter (`JpaRepository`) |
| **SQL'i kim üretir** | kimse (SQL yok) | sen | Hibernate |
| **DB bağlantısı gerekli mi** | hayır | evet (`DataSource`, `application.properties`) | evet (`DataSource` + Hibernate ayarları: `spring.jpa.hibernate.ddl-auto` vb.) |
| **Kalıcılık (restart sonrası veri durur mu)** | hayır, kaybolur | evet | evet |
| **Lazy loading / persistence context** | yok (kavram bile yok) | yok | var |
| **`getFirstName()` ihtiyacı** | Jackson icin gerekli (class ise) | Jackson icin gerekli (class ise); record'da otomatik | Jackson icin gerekli (field-based access'te Hibernate'e gerekmez ama Jackson'a gerekli) |
| **Bu repoda örnek** | `01-books`, `02-books` | `employee-jdbc-security` | `employees`, `employee-security`, `todos` |
| **Okul projesinde örnek** | — | `BuroBOVProject-review` | — |

---

## Kısaca sonuç

Sorduğun "3 kategori mi var" sorusuna cevap: **evet, ama senin tahmin ettiğin
ayrım noktası biraz farklı.** `todos` bir "4. kategori" (JPA Data diye ayrı
bir şey) değil — `employees`/`employee-security` ile **aynı kategoride**
(Spring Data JPA), henüz kodu yazılmadığı için boş görünüyor. Asıl 3. kategori
aslında `01-books`/`02-books`'un temsil ettiği **"in-memory, veritabanı bile
yok"** yaklaşımı — bunu muhtemelen "zaten çalışıyor, bir kategorisi vardır"
diye es geçmiştin.
