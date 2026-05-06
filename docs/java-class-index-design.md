# Java Class Index Design Memo

## Purpose

Agent Skills で Java コードベースを扱うときに、`.java` ソースを大量に読ませず、`.class` / `.jar` から得られる情報をあらかじめ索引化しておく。

目的は次の通り。

- Agent が依存関係やクラス構造を低トークンで把握できるようにする
- 必要な class / method / field だけを素早く発見できるようにする
- `rg` / `jq` で扱いやすい JSON / JSONL 生成物にする
- Java reflection に近い見え方で class 情報を整理する

## Product Position

このツールは miku シリーズとして扱う。

ただし、他の miku シリーズのように Node.js / TypeScript upstream から Java 版へ straight conversion する形ではない。

このツールは最初から Java-only の miku product として設計する。

理由は次の通り。

- 主入力が `.class` / `.jar` / classpath であり、Java runtime と bytecode model が中心になる
- ClassGraph / ASM / jdeps など、Java 側の toolchain と密接に結びつく
- Agent Skills 向け出力は JSON / JSONL だが、解析本体は Java で持つ方が自然
- Node.js upstream を別に置くと、bytecode 解析の意味論が二重管理になりやすい

したがって、この project では `upstream file -> Java class` の straight conversion 対応表を主軸にしない。

代わりに、Java implementation 自体を canonical implementation とし、CLI / Agent Skills / MCP などを Java-generated artifacts の利用 surface として扱う。

Maven plugin は first-class surface として対応したい。

Java-only の miku product であり、入力が `.class` / `.jar` / classpath であるため、Maven build lifecycle から index を生成できる価値が高い。

CLI だけでなく、Maven plugin からも同じ core implementation を呼び出す。

## Direction

既存 OSS をそのまま使うより、`.class` / `.jar` を読む小さな indexer を自作する方針がよい。

土台候補は次の通り。

- `ClassGraph`
  - `.class` / `.jar` / classpath / module path を高速に読む
  - class / interface / annotation / method / field / inheritance を扱える
  - class をロード・初期化せずに bytecode から情報を取れる
  - reflection-like な索引を作る土台として有力
- `ASM`
  - method body 内の呼び出し、field access、new している型など、bytecode instruction まで読みたい場合に使う
  - call graph や detailed dependency を作る段階で有力
- `jdeps`
  - JDK 標準で class / jar の依存関係を見られる
  - JSON ではなく DOT / text 寄りなので、今回の主軸ではない
- `Jarviz`
  - method level coupling を JSON Lines で出す既存 OSS
  - reflection-like JSON ではなく、`sourceClass/sourceMethod -> targetClass/targetMethod` の結合グラフ寄り

MVP で主に使う OSS は `Jandex` と `ASM` とする。

```text
Jandex:
  class / interface / enum / annotation
  package
  superclass / interfaces
  fields
  methods / constructors
  annotations
  generic signature
  inheritance hierarchy
  offline reflection 的な構造

ASM:
  method body の invoke 系 opcode
  methodCall
  constructorCall
```

つまり、Pass 1 では Jandex で class JSON の土台を作り、Pass 2 では ASM で `calls[]` と `methodCall` / `constructorCall` edge を足す。

## Execution Surfaces

Core implementation は Java library として持つ。

その上に、少なくとも CLI と Maven plugin を載せる。

```text
core Java library
  -> CLI
  -> Maven plugin
  -> future MCP / Agent Skills helper
```

### CLI

CLI は手元の jar / classes directory を直接指定して index を生成する。

```text
miku-class-index index \
  --input target/classes \
  --output .java-class-index

miku-class-index index \
  --input target/example.jar \
  --output .java-class-index
```

CLI は単体調査、CI、Agent からの明示実行に向く。

### Maven Plugin

Maven plugin は Maven project の build output から index を生成する。

想定する short form。

```text
mvn miku-class-index:index
```

artifactId / prefix の候補。

```text
artifactId: miku-class-index-maven-plugin
prefix: miku-class-index
goal: index
```

基本設定の例。

```xml
<plugin>
  <groupId>jp.igapyon</groupId>
  <artifactId>miku-class-index-maven-plugin</artifactId>
  <version>${miku-class-index.version}</version>
  <configuration>
    <outputDirectory>${project.basedir}/.java-class-index</outputDirectory>
    <includeTestClasses>false</includeTestClasses>
    <includeDependencies>false</includeDependencies>
  </configuration>
</plugin>
```

初期 goal は `index` のみでよい。

```text
index:
  target/classes を主入力として .java-class-index を生成する

future:
  index-test:
    target/test-classes も含める
  index-dependencies:
    dependency jar も含める
  clean-index:
    generated index を削除する
```

Maven plugin では、最初から build lifecycle に強く bind しすぎない。

まずは明示実行を基本にする。

```text
mvn miku-class-index:index
```

必要になったら `process-classes` または `verify` への bind 例を documentation で示す。

```xml
<executions>
  <execution>
    <phase>process-classes</phase>
    <goals>
      <goal>index</goal>
    </goals>
  </execution>
</executions>
```

Maven plugin が扱う主な parameter。

```text
outputDirectory
classesDirectory
testClassesDirectory
includeTestClasses
includeDependencies
includeScopes
excludePackages
includePackages
failOnMissingClasses
formatJson
formatJsonl
```

Maven plugin 対応の理由。

- Java project では `target/classes` が自然な入力になる
- compile 後の `.class` をそのまま解析できる
- CI や release artifact 生成に組み込みやすい
- Agent Skills 用 index を build artifact として安定生成できる
- Java-only miku product として Maven ecosystem に自然に乗る

## External Package Boundary

`java.*` / `javax.*` などの標準API・外部APIパッケージは、原則として中を解析しない。

ただし、依存関係としては記録する。

```text
do:
  jp.igapyon.example.FooService -> java.util.List
  jp.igapyon.example.WebController -> javax.servlet.http.HttpServletRequest

do not:
  java.util.List の methods / fields / dependencies を展開する
  javax.servlet.http.HttpServletRequest の内部 class graph を展開する
```

つまり、これらは graph の leaf または external node として扱う。

```jsonl
{"from":"jp.igapyon.example.FooService","to":"java.util.List","relation":"methodReturnType","targetKind":"external-platform"}
{"from":"jp.igapyon.example.WebController","to":"javax.servlet.http.HttpServletRequest","relation":"methodParameterType","targetKind":"external-api"}
```

初期 exclude / no-descend package の候補。

```text
java.*
javax.*
jakarta.*
jdk.*
sun.*
com.sun.*
org.w3c.*
org.xml.*
```

注意点として、`javax.*` は古い Java EE / Jakarta EE 系 API を含む。JDK 標準APIとして常に存在するものとは限らない。

そのため `javax.*` は「無視」ではなく「外部APIとして edge は残す」が重要。

同様に、`jakarta.*` や framework API も、中を読まない設定にできるようにする。

```text
noDescendPackages:
  - java.*
  - javax.*
  - jakarta.*
  - org.springframework.*
```

`noDescendPackages` は「対象パッケージの class index file を生成しない」という意味に限定する。

参照元 class の fields / methods / annotations に現れた型名は、dependencies として記録する。

## Output Layout

class ごとに JSON を分ける。

理由は次の通り。

- 必要な class だけ読める
- 1ファイルが小さく、Agent のトークン消費を抑えやすい
- git diff が見やすい
- incremental 更新しやすい
- `rg` で class 名や method 名から到達しやすい
- 巨大な単一 JSON の破損や競合を避けられる
- `jq` で class 単位に機械処理しやすい

推奨レイアウト。

```text
.java-class-index/
  index.json
  symbols.jsonl
  dependencies.jsonl
  classes/
    jp/
      igapyon/
        example/
          FooService.json
          FooService$Inner.json
          BarRepository.json
```

package は directory で表す。

```text
classes/jp/igapyon/example/FooService.json
=> jp.igapyon.example.FooService
```

内部クラスは JVM の binary name に合わせて `$` を使う。

```text
FooService$Inner.json
FooService$Inner$Nested.json
```

## Class JSON File Format

class JSON には FQCN を必ず入れる。

パスから FQCN は復元できるが、Agent や `rg` は本文検索するため、本文にも明示した方が検索しやすい。

例。

```json
{
  "schemaVersion": "java-class-index-class-v1",
  "identity": {
    "binaryName": "jp.igapyon.example.FooService",
    "canonicalName": "jp.igapyon.example.FooService",
    "packageName": "jp.igapyon.example",
    "simpleName": "FooService",
    "kind": "class",
    "modifiers": ["public", "final"],
    "classFileName": "FooService.class",
    "sourceArtifact": "target/example.jar"
  },
  "inheritance": {
    "superClass": "java.lang.Object",
    "interfaces": [
      "jp.igapyon.example.FooApi"
    ]
  },
  "annotations": [
    {
      "type": "org.springframework.stereotype.Service",
      "values": {}
    }
  ],
  "fields": [
    {
      "name": "repository",
      "type": "jp.igapyon.example.BarRepository",
      "modifiers": ["private", "final"],
      "annotations": []
    }
  ],
  "constructors": [
    {
      "signature": "FooService(jp.igapyon.example.BarRepository)",
      "modifiers": ["public"],
      "parameterTypes": [
        "jp.igapyon.example.BarRepository"
      ]
    }
  ],
  "methods": [
    {
      "name": "findAll",
      "signature": "java.util.List<jp.igapyon.example.Foo> findAll()",
      "returnType": "java.util.List",
      "genericReturnType": "java.util.List<jp.igapyon.example.Foo>",
      "parameterTypes": [],
      "modifiers": ["public"],
      "throws": [],
      "annotations": []
    }
  ],
  "dependencies": [
    {
      "to": "java.lang.Object",
      "relation": "extends",
      "targetKind": "external-platform"
    },
    {
      "to": "jp.igapyon.example.FooApi",
      "relation": "implements",
      "targetKind": "internal"
    },
    {
      "to": "jp.igapyon.example.BarRepository",
      "relation": "fieldType",
      "member": "repository",
      "targetKind": "internal"
    },
    {
      "to": "java.util.List",
      "relation": "methodReturnType",
      "member": "findAll",
      "targetKind": "external-platform"
    },
    {
      "to": "jp.igapyon.example.Foo",
      "relation": "genericSignatureType",
      "member": "findAll",
      "targetKind": "internal"
    }
  ]
}
```

`binaryName` と `canonicalName` は分ける。

```text
binaryName:
  jp.igapyon.example.FooService$Inner
  .class / JVM / Class#getName() に近い

canonicalName:
  jp.igapyon.example.FooService.Inner
  Java ソース表記 / Class#getCanonicalName() に近い
```

Agent 検索では `$` でも `.` でも引っかかるように、両方を持つのが実用的。

内部クラスの例。

```json
{
  "schemaVersion": "java-class-index-class-v1",
  "identity": {
    "binaryName": "jp.igapyon.example.FooService$Inner",
    "canonicalName": "jp.igapyon.example.FooService.Inner",
    "packageName": "jp.igapyon.example",
    "simpleName": "Inner",
    "kind": "class",
    "classFileName": "FooService$Inner.class",
    "outerClass": "jp.igapyon.example.FooService"
  }
}
```

`java.lang.String` や `java.util.List` のような external package class については、参照されたことだけを dependency として記録し、class JSON は生成しない。

```text
do:
  classes/jp/igapyon/example/FooService.json を作る
  FooService.json の dependencies に java.lang.String を書く

do not:
  classes/java/lang/String.json を作る
  java.lang.String の fields / methods を reflection して展開する
```

## Sample Class JSON

class ごとの JSON 正本の例。

```json
{
  "schemaVersion": "java-class-index-class-v1",
  "identity": {
    "binaryName": "jp.igapyon.example.FooService",
    "canonicalName": "jp.igapyon.example.FooService",
    "packageName": "jp.igapyon.example",
    "simpleName": "FooService",
    "kind": "class",
    "modifiers": ["public", "final"],
    "classFileName": "FooService.class",
    "sourceArtifact": "target/example.jar"
  },
  "inheritance": {
    "superClass": "java.lang.Object",
    "interfaces": ["jp.igapyon.example.FooApi"]
  },
  "annotations": [
    {
      "type": "org.springframework.stereotype.Service",
      "values": {}
    }
  ],
  "fields": [
    {
      "name": "repository",
      "type": "jp.igapyon.example.BarRepository",
      "modifiers": ["private", "final"],
      "annotations": []
    }
  ],
  "constructors": [
    {
      "name": "<init>",
      "signature": "FooService(jp.igapyon.example.BarRepository)",
      "modifiers": ["public"],
      "parameterTypes": ["jp.igapyon.example.BarRepository"],
      "calls": [
        {
          "toClass": "java.lang.Object",
          "toMethod": "<init>()",
          "opcode": "invokespecial",
          "targetKind": "external-platform"
        }
      ]
    }
  ],
  "methods": [
    {
      "name": "findAll",
      "signature": "java.util.List<jp.igapyon.example.Foo> findAll()",
      "descriptor": "()Ljava/util/List;",
      "returnType": "java.util.List",
      "genericReturnType": "java.util.List<jp.igapyon.example.Foo>",
      "parameterTypes": [],
      "modifiers": ["public"],
      "throws": [],
      "annotations": [],
      "calls": [
        {
          "toClass": "jp.igapyon.example.BarRepository",
          "toMethod": "findAll()",
          "opcode": "invokeinterface",
          "targetKind": "internal"
        }
      ]
    },
    {
      "name": "findById",
      "signature": "jp.igapyon.example.Foo findById(java.lang.String)",
      "descriptor": "(Ljava/lang/String;)Ljp/igapyon/example/Foo;",
      "returnType": "jp.igapyon.example.Foo",
      "parameterTypes": ["java.lang.String"],
      "modifiers": ["public"],
      "throws": [],
      "annotations": [],
      "calls": [
        {
          "toClass": "jp.igapyon.example.BarRepository",
          "toMethod": "findById(java.lang.String)",
          "opcode": "invokeinterface",
          "targetKind": "internal"
        }
      ]
    }
  ],
  "dependencies": [
    {
      "to": "java.lang.Object",
      "relation": "extends",
      "targetKind": "external-platform"
    },
    {
      "to": "jp.igapyon.example.FooApi",
      "relation": "implements",
      "targetKind": "internal"
    },
    {
      "to": "org.springframework.stereotype.Service",
      "relation": "annotationType",
      "targetKind": "external-api"
    },
    {
      "to": "jp.igapyon.example.BarRepository",
      "relation": "fieldType",
      "member": "repository",
      "targetKind": "internal"
    },
    {
      "to": "java.util.List",
      "relation": "methodReturnType",
      "member": "findAll",
      "targetKind": "external-platform"
    },
    {
      "to": "jp.igapyon.example.Foo",
      "relation": "genericSignatureType",
      "member": "findAll",
      "targetKind": "internal"
    },
    {
      "to": "java.lang.String",
      "relation": "methodParameterType",
      "member": "findById",
      "targetKind": "external-platform"
    },
    {
      "to": "jp.igapyon.example.BarRepository",
      "relation": "methodCall",
      "member": "findAll",
      "targetMethod": "findAll()",
      "targetKind": "internal"
    }
  ]
}
```

内部クラスの JSON 正本の例。

```json
{
  "schemaVersion": "java-class-index-class-v1",
  "identity": {
    "binaryName": "jp.igapyon.example.FooService$Inner",
    "canonicalName": "jp.igapyon.example.FooService.Inner",
    "packageName": "jp.igapyon.example",
    "simpleName": "Inner",
    "kind": "class",
    "modifiers": ["public", "static"],
    "classFileName": "FooService$Inner.class",
    "sourceArtifact": "target/example.jar",
    "outerClass": "jp.igapyon.example.FooService"
  },
  "inheritance": {
    "superClass": "java.lang.Object",
    "interfaces": []
  },
  "annotations": [],
  "fields": [],
  "constructors": [],
  "methods": [],
  "dependencies": [
    {
      "to": "java.lang.Object",
      "relation": "extends",
      "targetKind": "external-platform"
    },
    {
      "to": "jp.igapyon.example.FooService",
      "relation": "outerClass",
      "targetKind": "internal"
    }
  ]
}
```

## Index JSON And JSONL Files

class ごとの JSON を正本にし、検索用には薄い index JSON / JSONL を併設する。

### index.json

FQCN と class JSON path の対応を持つ discovery index。

```json
{
  "schemaVersion": "java-class-index-v1",
  "classes": [
    {
      "binaryName": "jp.igapyon.example.FooService",
      "canonicalName": "jp.igapyon.example.FooService",
      "path": "classes/jp/igapyon/example/FooService.json",
      "kind": "class"
    },
    {
      "binaryName": "jp.igapyon.example.FooService$Inner",
      "canonicalName": "jp.igapyon.example.FooService.Inner",
      "path": "classes/jp/igapyon/example/FooService$Inner.json",
      "kind": "class"
    }
  ]
}
```

### symbols.jsonl

symbol 検索用。`rg` や JSONL reader で小さく検索できる。

```jsonl
{"kind":"class","name":"jp.igapyon.example.FooService","path":"classes/jp/igapyon/example/FooService.json"}
{"kind":"field","owner":"jp.igapyon.example.FooService","name":"repository","type":"jp.igapyon.example.BarRepository","path":"classes/jp/igapyon/example/FooService.json"}
{"kind":"method","owner":"jp.igapyon.example.FooService","name":"findAll","signature":"public java.util.List<jp.igapyon.example.Foo> findAll()","path":"classes/jp/igapyon/example/FooService.json"}
```

### dependencies.jsonl

依存関係グラフ用。

```jsonl
{"from":"jp.igapyon.example.FooService","to":"java.lang.Object","relation":"extends"}
{"from":"jp.igapyon.example.FooService","to":"jp.igapyon.example.FooApi","relation":"implements"}
{"from":"jp.igapyon.example.FooService","to":"jp.igapyon.example.BarRepository","relation":"fieldType","member":"repository"}
{"from":"jp.igapyon.example.FooService","to":"java.util.List","relation":"methodReturnType","member":"findAll"}
```

## Reflection Mapping

JSON の項目は Java reflection に寄せる。

```text
Class<?>                         -> class entry
Class#getName()                  -> binaryName
Class#getCanonicalName()         -> canonicalName
Class#getPackageName()           -> packageName
Class#getSimpleName()            -> simpleName
Class#getModifiers()             -> modifiers
Class#getSuperclass()            -> superClass
Class#getInterfaces()            -> interfaces
Class#getDeclaredFields()        -> fields
Class#getDeclaredMethods()       -> methods
Class#getDeclaredConstructors()  -> constructors
AnnotatedElement#getAnnotations()-> annotations
```

`.class` から取れる情報。

```text
取れる:
  class / interface / enum / annotation
  modifiers
  superclass / interfaces
  fields / methods / constructors
  parameter types
  return type
  throws
  annotations
  generic signature が残っていれば generics

条件付き:
  parameter names は -parameters 付き compile なら綺麗に取れる

reflection 風モデルだけでは普通扱わないが bytecode 解析なら取れる:
  メソッド内の呼び出し先
  new している型
  field access
```

## Design Rule

当面の正本は class ごとの JSON とする。

```text
package = directory
class file = SimpleName.json
inner class = Outer$Inner.json
FQCN = class JSON identity + index.json に持つ
external package class = dependency としては記録するが class JSON は生成しない
```

`index.json` / `symbols.jsonl` / `dependencies.jsonl` は派生物として扱う。

将来的に method body の call graph まで必要になったら、ClassGraph だけでなく ASM を併用する。

## Method Body Traversal MVP

`.class` を traverse するとき、method body の中身は MVP では「呼び出し」だけを見る。

制御構造は MVP から外す。

```text
include in MVP:
  method invocation
  constructor invocation

exclude from MVP:
  if / switch
  for / while
  try / catch / finally
  control-flow graph
  data-flow analysis
  local variable lifetime
  branch condition
  stack simulation beyond what is needed for call target extraction
```

bytecode instruction としては、まず invocation 系 opcode を edge にする。

```text
invokevirtual
invokespecial
invokestatic
invokeinterface
invokedynamic
```

MVP の出力例。

```jsonl
{"fromClass":"jp.igapyon.example.FooService","fromMethod":"findAll()","toClass":"jp.igapyon.example.BarRepository","toMethod":"findAll()","relation":"methodCall","opcode":"invokeinterface"}
{"fromClass":"jp.igapyon.example.FooService","fromMethod":"createFoo()","toClass":"jp.igapyon.example.Foo","toMethod":"<init>(java.lang.String)","relation":"constructorCall","opcode":"invokespecial"}
```

`if` や `for` などを見ない理由。

- Agent がまず欲しいのは「どの method がどの method を呼ぶか」
- 制御フローを入れると JSON が大きくなり、MVP の目的である低トークン索引から外れやすい
- call graph と class dependency が安定してから、必要に応じて CFG / data-flow を別 artifact として追加できる

したがって、method body traversal の初期責務は call edge extraction に限定する。

## Dependency Relations

依存関係は method call だけではない。

MVP では、依存関係を大きく次の 2 種類に分ける。

```text
type dependency:
  class / field / method signature / annotation に現れる型への依存

call dependency:
  method body の invocation opcode から得られる呼び出し依存
```

type dependency は Jandex で取る。

```text
class-level:
  extends
  implements
  annotationType

field-level:
  fieldType
  fieldAnnotationType
  genericFieldType

method-level:
  methodReturnType
  methodParameterType
  methodThrowsType
  methodAnnotationType
  genericMethodType

constructor-level:
  constructorParameterType
  constructorThrowsType
  constructorAnnotationType
```

call dependency は ASM で取る。

```text
body-level:
  methodCall
  constructorCall
```

例。

```java
class FooService {
  private final BarRepository repository;

  List<Foo> findAll(String tenantId) {
    return repository.findAll(tenantId);
  }
}
```

この場合、少なくとも次の dependency edge がありえる。

```jsonl
{"from":"FooService","to":"BarRepository","relation":"fieldType","member":"repository"}
{"from":"FooService","to":"java.util.List","relation":"methodReturnType","member":"findAll"}
{"from":"FooService","to":"Foo","relation":"genericMethodType","member":"findAll"}
{"from":"FooService","to":"java.lang.String","relation":"methodParameterType","member":"findAll"}
{"from":"FooService.findAll","to":"BarRepository.findAll","relation":"methodCall","targetKind":"internal"}
```

`fieldType` は重要な dependency として扱う。

DI や Service / Repository 構成では、field や constructor parameter に設計上の結合が現れる。method call だけを見ると、設計上の依存を見落とす。

したがって、MVP の dependency graph は次を両方持つ。

```text
FooService -> BarRepository
  relation: fieldType

FooService.findAll -> BarRepository.findAll
  relation: methodCall
```

## Multi-pass Graph Generation

Graph を作る場合も、少なくとも 2 パスに分ける。

1 パス目では、class 単位の identity と symbol table を確定する。

```text
Pass 1: Class / Symbol discovery

input:
  .class / .jar / classpath

output:
  class identity
  package
  binaryName
  canonicalName
  simpleName
  kind
  modifiers
  class file path
  source artifact
  declared fields
  declared constructors
  declared methods
  annotations
  inheritance surface
  symbols.jsonl の元データ
```

この段階では、依存関係の edge を急いで確定しない。

まず「存在する class / member は何か」を安定した辞書として固める。これにより、2 パス目で参照先を解決するときに、内部 class、同名 simple class、外部 dependency、missing class を分けやすくなる。

2 パス目では、1 パス目で作った symbol table を使って dependency / graph edge を張る。

```text
Pass 2: Dependency / Graph resolution

input:
  Pass 1 symbol table
  parsed class metadata
  bytecode invocation scan

output:
  dependencies.jsonl
  class -> class edges
  member -> type edges
  method -> method call edges
  missing / external dependency records
```

最初の dependency は reflection-like surface から作る。

```text
extends
implements
fieldType
constructorParameterType
methodParameterType
methodReturnType
throwsType
annotationType
genericSignatureType
```

method body からは invocation edge だけを取る。

```text
mvp:
  methodCall
  constructorCall

optional later:
  fieldRead
  fieldWrite
  newInstance
  controlFlow
  dataFlow
```

2 パスにする理由。

- class identity と dependency edge の責務を分けられる
- 参照先解決時に全 class の存在を前提にできる
- inner class や duplicate simple name を安全に扱える
- missing dependency を edge として記録しやすい
- incremental 更新時に、class summary 更新と graph 再解決を分けられる
- Agent 向け class JSON と machine-readable graph を別々に安定生成できる

将来的には 3 パス構成も考えられる。

```text
Pass 1: class / member discovery
Pass 2: type dependency resolution + invocation edge resolution
Pass 3: optional advanced bytecode analysis such as field access / CFG / data-flow
```

ただし初期設計では 2 パスを最低ラインとし、method invocation edge は Pass 2 に含める。Pass 3 相当は optional として扱う。
