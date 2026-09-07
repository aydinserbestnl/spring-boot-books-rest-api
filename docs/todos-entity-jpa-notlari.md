# `todos` Projesi: Todo Entity'si — record mu class mı, ve "required by JPA" ne demek

Bu doküman iki soruya cevap veriyor:
1. `Todo` sınıfı için record mu class mı tercih edilmeli, neden.
2. Koddaki `// default constructor required by JPA` yorumu ne anlama geliyor,
   bu proje gerçekten JPA ile mi yazılıyor, düz JDBC'den pratik farkı ne, ve
   bundan sonra hangi "required by JPA" durumlarıyla karşılaşman muhtemel.

İncelenen dosya: `todos/src/main/java/com/luv2code/springboot/todos/entity/Todo.java`

---

## 1) Record mu, class mı? — Bu proje için cevap kesin: **class, tartışmasız**

Önceki projelerde (`employee-jdbc-security`) DTO/entity'ler için record'u
önermiştim çünkü orada JPA yoktu. Burada durum tam tersi: `Todo` sınıfının
üzerinde `@Entity` var:

```java
@Table(name = "todos")
@Entity
public class Todo {
```

`@Entity` olan bir sınıf **record olamaz** — bu bir zevk/stil meselesi değil,
teknik bir zorunluluk. Üç somut sebebi var, ve ilginç olan şu: **kodundaki
üç farklı satır, bu üç sebebin her birini ayrı ayrı kanıtlıyor.**

### Sebep 1 — Parametresiz constructor zorunluluğu

Kodun 23-25. satırları:
```java
//default constructor required by JPA
public Todo() {
}
```

JPA spesifikasyonu, her entity'nin **public veya protected, parametresiz bir
constructor**'a sahip olmasını **şart koşuyor**. Sebebi: Hibernate, veritabanından
bir satır okuduğunda senin `Todo(String title, String description, ...)`
gibi parametreli constructor'ını **kullanmıyor** — önce nesneyi reflection ile
**boş** olarak yaratıyor (`new Todo()`), sonra kolon değerlerini tek tek
field'lara yerleştiriyor (field-based access ile doğrudan, ya da setter'lar
üzerinden).

Record'da parametresiz constructor **yoktur ve olamaz** — bir record'un
constructor'ı her zaman tüm component'leri (yani `id`, `title`, `description`,
`priority`, `completed`) parametre olarak alır. Bu yüzden Hibernate'in
ihtiyaç duyduğu "önce boş oluştur" adımını record ile yapamazsın.

### Sebep 2 — Alan sonradan (constructor bittikten sonra) değişebilmeli

```java
@Id
@GeneratedValue(strategy = GenerationType.AUTO)
@Column(nullable = false)
private Long id;
```

`id` alanı `@GeneratedValue` ile işaretli — yani sen `new Todo(...)` dediğinde
`id` henüz **bilinmiyor** (veritabanı INSERT sonrası üretecek). Hibernate,
INSERT çalıştıktan **sonra**, veritabanının ürettiği gerçek `id` değerini
**geri gelip senin nesnene yazıyor** (`setId(...)` ya da doğrudan field'a
reflection ile). Bu, nesnenin constructor'dan **sonra da değiştirilebilir**
(mutable) olmasını gerektiriyor. Record'da tüm field'lar `final` — constructor
bittikten sonra hiçbir alan değiştirilemez, dolayısıyla Hibernate'in
"id'yi sonradan yaz" adımını da record ile yapamazsın.

### Sebep 3 — Lazy loading için proxy/alt sınıf ihtiyacı

Kodda şu an yorum satırında duran, ileride ekleyeceğin alan:
```java
// private User owner; // ... implement security'de eklenecek
```

Bunu `@ManyToOne(fetch = FetchType.LAZY)` ile eklediğinde, Hibernate bazen
senin `Todo` sınıfının **bir alt sınıfını (proxy)** üretip `owner` alanına
gerçekten ne zaman erişildiğini yakalamak için kullanır (lazy loading'in
mekanizması budur). Bunun için sınıfının **`final` olmaması** gerekir. Record
sınıfları Java tarafından örtük olarak `final`'dır — bu yüzden proxy
üretilemez.

**Sonuç:** Bu üç sebepten herhangi biri tek başına yeterli, üçü birden
olunca konu tartışmaya bile açık değil — `Todo` (ve JPA ile işaretlenmiş her
entity) class kalmalı. Buna karşın ileride yazacağın `TodoRequest` (istek
DTO'su, `@Entity` DEĞİL) yine `record` olabilir/olmalı — tıpkı
`employee-jdbc-security`'deki `EmployeeRequest` gibi.

---

## 2) Bu proje gerçekten JPA ile mi yazılıyor?

Evet. Kanıtlar:

**`pom.xml`:**
```xml
<artifactId>spring-boot-starter-data-jpa</artifactId>
```

**`Todo.java`'daki dört annotasyon:**
- `@Entity` — bu class'ın Hibernate tarafından yönetilen bir "tablo satırı
  şablonu" olduğunu bildirir.
- `@Table(name = "todos")` — hangi tabloya karşılık geldiğini söyler.
- `@Id` + `@GeneratedValue(strategy = GenerationType.AUTO)` — `id` alanının
  primary key olduğunu ve değerinin veritabanı tarafından otomatik
  üretileceğini söyler.
- `@Column(nullable = false)` — bu alanın DB şemasında `NOT NULL` olacağını
  söyler (ddl-auto ile tablo otomatik oluşturulursa).

Bu, `employees` ve `employee-security` projeleriyle **aynı kategori**
(Spring Data JPA / Hibernate) — daha önceki `docs/veri-erisim-katmanlari-3-kategori.md`
dokümanındaki Kategori 3'ün tam karşılığı.

## Pratikte "JPA ile yazmak" ne demek — düz JDBC'den farkı

| | JPA (`todos`, `employees`) | Düz JDBC (`employee-jdbc-security`) |
|---|---|---|
| Tablo/kolon eşleşmesi | `@Table`, `@Column` ile deklaratif | SQL metninde elle (`"select ... from todos"`) |
| SQL'i kim yazar | Hibernate (sen hiç yazmazsın, `JpaRepository` kullanırsan) | Sen (`JdbcTemplate` + `RowMapper`) |
| Nesne nasıl oluşturuluyor | Boş oluştur (`new Todo()`) → alanları sonradan doldur | Constructor'a tüm değerleri tek seferde ver |
| Entity sınıfı record olabilir mi | **Hayır** (yukarıdaki 3 sebep) | Evet (annotasyon/kısıtlama yok) |
| Kalıcılık takibi | Var (persistence context, dirty checking) | Yok, her SQL çağrısı bağımsız |

---

## 3) Şu ana kadar karşılaştığın "required by JPA": parametresiz constructor

Zaten yazdın, doğru. Ama bu tek gereklilik değil — projeyi ilerlettikçe
karşına çıkması **çok muhtemel** başka JPA gereklilikleri var:

### a) `Long id` kullanman — kendi projendeki geçmiş bir hatayla birebir aynı risk

`Todo.java`'da:
```java
private Long id;   // wrapper (Long), primitive (long) degil
```

Bu, tesadüfen, senin **kendi `employees` projendeki dokümante edilmiş bir
hatanın** başlangıç durumuyla birebir aynı (`employees/docs/save-transactional-id-notlari.md`
dosyasında tüm hikaye anlatılı). Özet: Hibernate, `@GeneratedValue`'lu bir
`id` alanının "hiç kaydedilmemiş mi" olduğuna karar verirken şu kurala bakar:

| `id` tipi | "Yeni kayıt" sayılan değer |
|---|---|
| `Long` (wrapper) | sadece `null` |
| `long` (primitive) | `0` |

Yani `Long id` kullandığın için, yeni bir `Todo` oluştururken **`id`'yi hiç
set etmeden `null` bırakman** gerekiyor — eğer ileride (video seni oraya
götürürse) `new Todo()` yaratıp `id`'ye yanlışlıkla `0L` verirsen, Hibernate
bunu "var olan ama veritabanında bulunamayan bir kayıt" sanıp şu hatayı
fırlatabilir:
```
org.hibernate.StaleObjectStateException: Row was already updated or deleted...
```
Bu tam olarak `employees` projesinde yaşanmış, çözülmüş ve belgelenmiş hata.
Şimdiden bilmen, aynı hatayı tekrar çözmek zorunda kalmamanı sağlar.

### b) Yazma (save/delete) işlemleri için `@Transactional` ihtiyacı — hangi yolu seçtiğine bağlı

İki farklı yol var, videonun hangisini izlediğine göre:

- **`employees` tarzı (elle `EntityManager` kullanan bir DAO yazarsan):**
  `save()`/`deleteById()` metotlarına **elle `@Transactional` eklemen
  gerekecek**, yoksa şu hatayı alırsın:
  ```
  jakarta.persistence.TransactionRequiredException:
  No EntityManager with actual transaction available for current thread
  ```
- **`employee-security` tarzı (`JpaRepository<Todo, Long>` interface'i
  extend edersen):** Bu, Spring Data JPA'nın hazır `SimpleJpaRepository`
  implementasyonunu kullanır, `@Transactional` **zaten içeride otomatik
  var** — sen hiçbir şey eklemene gerek kalmaz.

### c) `private User owner` eklediğinde — ilişki annotasyonları

Şu an yorum satırında duran alanı gerçek koda çevirdiğinde:
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "owner_id")
private User owner;
```
gibi ek annotasyonlara ihtiyacın olacak — `@ManyToOne` (bir todo'nun tek bir
sahibi olur), `@JoinColumn` (foreign key kolonunun adı), ve muhtemelen
`fetch = FetchType.LAZY` (owner bilgisini her Todo çekişinde otomatik
yüklememek için — tam da bu yüzden Todo sınıfının record olmaması gerektiğini
yukarıda anlattık, LAZY ilişkiler proxy/final-olmayan sınıf gerektiriyor).

### d) Veritabanı bağlantısı henüz hiç yok — bir sonraki adımın bu olacak

`todos/src/main/resources/application.properties` şu an sadece:
```properties
spring.application.name=todos
```
İçeriyor — hiç `spring.datasource.*` yok. Ama `pom.xml`'de
`mysql-connector-j` bağımlılığı var (H2 değil, gerçek bir MySQL'e
bağlanmak üzere kurulmuş). Yani projeyi ilk çalıştırdığında muhtemelen şu
hatayı alacaksın:
```
Failed to configure a DataSource: 'url' attribute is not specified
```
Bu "required by JPA" değil, "required by Spring Boot" — ama sırada seni
bekleyen bir sonraki adım kesinlikle bu: `spring.datasource.url`,
`username`, `password` (ve muhtemelen `spring.jpa.hibernate.ddl-auto`)
satırlarını eklemen gerekecek.

---

## Özet — beklenen sıradaki "required by JPA" listesi

1. ~~Parametresiz constructor~~ — zaten yaptın.
2. `id` için `Long`/`null` disiplinine dikkat et (ya da `employees`'in
   sonunda yaptığı gibi `long`/`0`'a geçebilirsin) — aksi halde
   `StaleObjectStateException` riski var.
3. DAO'yu elle mi (`EntityManager`) yoksa `JpaRepository` ile mi yazacağına
   göre, `@Transactional` ihtiyacı değişir.
4. `User owner` eklenince `@ManyToOne` + `@JoinColumn` (+ muhtemelen
   `FetchType.LAZY`) gerekecek.
5. `application.properties`'e gerçek bir `spring.datasource.*` bağlantısı
   eklenmeden uygulama açılışta hata verecek.
