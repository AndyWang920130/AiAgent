/**
 * Convert a JSON value into Java entity class source. Runs entirely in the browser —
 * no network, no backend. Generates one class per distinct JSON object shape, with
 * Jackson-friendly fields (getters/setters, and @JsonProperty when the JSON key is not
 * a plain camelCase Java identifier).
 *
 * Type inference:
 *   - string  → String
 *   - integer → Long, decimal → Double
 *   - boolean → Boolean
 *   - object  → a nested generated class (named after the property key)
 *   - array   → List<T>, T inferred from the first element (Object when empty/mixed)
 *   - null / unknown → Object
 */

export interface JsonToJavaOptions {
  /** Name of the top-level class. */
  rootClassName: string
  /** Package declaration to emit (blank = none). */
  packageName?: string
}

/** A resolved Java field: its Java name, type, and the original JSON key (for @JsonProperty). */
interface JavaField {
  javaName: string
  javaType: string
  jsonKey: string
}

const JAVA_KEYWORDS = new Set([
  'abstract', 'assert', 'boolean', 'break', 'byte', 'case', 'catch', 'char', 'class',
  'const', 'continue', 'default', 'do', 'double', 'else', 'enum', 'extends', 'final',
  'finally', 'float', 'for', 'goto', 'if', 'implements', 'import', 'instanceof', 'int',
  'interface', 'long', 'native', 'new', 'package', 'private', 'protected', 'public',
  'return', 'short', 'static', 'strictfp', 'super', 'switch', 'synchronized', 'this',
  'throw', 'throws', 'transient', 'try', 'void', 'volatile', 'while',
])

/** True when a JSON key is already a valid, idiomatic camelCase Java identifier. */
function isPlainJavaIdentifier(key: string): boolean {
  return /^[a-z][a-zA-Z0-9]*$/.test(key) && !JAVA_KEYWORDS.has(key)
}

/** Split a raw key into words on non-alphanumeric separators and camelCase boundaries. */
function splitWords(raw: string): string[] {
  return raw
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .split(/[^a-zA-Z0-9]+/)
    .filter(Boolean)
}

/** Convert a JSON key to a camelCase Java field name (fallback "field" if empty). */
function toFieldName(key: string): string {
  const words = splitWords(key)
  if (words.length === 0) return 'field'
  // Normalize every word to lowercase first so ALL_CAPS / acronym keys (DEVICE_ID, ID)
  // become proper camelCase (deviceId, id) rather than dEVICEId / iD.
  const name = words
    .map((w, i) => {
      const lower = w.toLowerCase()
      return i === 0 ? lower : lower.charAt(0).toUpperCase() + lower.slice(1)
    })
    .join('')
  // A field starting with a digit or hitting a keyword can't stand alone.
  if (/^[0-9]/.test(name)) return 'field' + name.charAt(0).toUpperCase() + name.slice(1)
  if (JAVA_KEYWORDS.has(name)) return name + '_'
  return name
}

/**
 * Reserve a unique field name within one class, suffixing with a counter on collision.
 * Two distinct JSON keys can normalize to the same camelCase name (e.g. `device_id` and
 * `DEVICE_ID` both → `deviceId`); without this the class would declare duplicate fields
 * and getters/setters and fail to compile. Each field keeps its own @JsonProperty, so the
 * distinct wire keys are still mapped correctly.
 */
function uniqueFieldName(used: Set<string>, base: string): string {
  let name = base
  let n = 2
  while (used.has(name)) {
    name = base + n++
  }
  used.add(name)
  return name
}

/** Convert a JSON key to a PascalCase Java class name (fallback "Item"). */
function toClassName(key: string): string {
  const words = splitWords(key)
  if (words.length === 0) return 'Item'
  const name = words.map(w => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase()).join('')
  return /^[0-9]/.test(name) ? 'C' + name : name
}

/** A generated class: its final name and the ordered fields it declares. */
interface GeneratedClass {
  name: string
  fields: JavaField[]
}

export function jsonToJava(json: unknown, options: JsonToJavaOptions): string {
  const rootName = toClassName(options.rootClassName || 'Root')
  const classes: GeneratedClass[] = []
  const usedNames = new Set<string>()

  /** Reserve a unique class name, suffixing with a counter on collision. */
  function uniqueClassName(base: string): string {
    let name = base || 'Item'
    let n = 1
    while (usedNames.has(name)) {
      name = base + n++
    }
    usedNames.add(name)
    return name
  }

  /**
   * Resolve the Java type for a JSON value. Objects recurse into a new generated class
   * named from `keyHint`; arrays produce List<element type>.
   */
  function resolveType(value: unknown, keyHint: string): string {
    if (value === null || value === undefined) return 'Object'
    if (typeof value === 'string') return 'String'
    if (typeof value === 'boolean') return 'Boolean'
    if (typeof value === 'number') return Number.isInteger(value) ? 'Long' : 'Double'
    if (Array.isArray(value)) {
      // Infer element type from the first non-null element; Object if none.
      const first = value.find(v => v !== null && v !== undefined)
      const elemType = first === undefined ? 'Object' : resolveType(first, singularize(keyHint))
      return `List<${elemType}>`
    }
    if (typeof value === 'object') {
      return buildClass(value as Record<string, unknown>, keyHint)
    }
    return 'Object'
  }

  /** Build a class from an object node and return its generated name. */
  function buildClass(obj: Record<string, unknown>, keyHint: string): string {
    const className = uniqueClassName(toClassName(keyHint))
    const fields: JavaField[] = []
    const usedFieldNames = new Set<string>()
    for (const [key, value] of Object.entries(obj)) {
      const javaType = resolveType(value, key)
      fields.push({ javaName: uniqueFieldName(usedFieldNames, toFieldName(key)), javaType, jsonKey: key })
    }
    classes.push({ name: className, fields })
    return className
  }

  // The root must be an object to produce a class; wrap arrays/scalars for a sensible result.
  let usesList = false
  if (json !== null && typeof json === 'object' && !Array.isArray(json)) {
    // Reserve the root name first so nested classes can't steal it.
    usedNames.add(rootName)
    const fields: JavaField[] = []
    const usedFieldNames = new Set<string>()
    for (const [key, value] of Object.entries(json as Record<string, unknown>)) {
      const javaType = resolveType(value, key)
      if (javaType.startsWith('List<')) usesList = true
      fields.push({ javaName: uniqueFieldName(usedFieldNames, toFieldName(key)), javaType, jsonKey: key })
    }
    classes.unshift({ name: rootName, fields })
  } else if (Array.isArray(json)) {
    usedNames.add(rootName)
    const first = (json as unknown[]).find(v => v !== null && v !== undefined)
    const elemType = first === undefined ? 'Object' : resolveType(first, rootName + 'Item')
    usesList = true
    classes.unshift({ name: rootName, fields: [{ javaName: 'items', javaType: `List<${elemType}>`, jsonKey: 'items' }] })
  } else {
    throw new Error('Root JSON must be an object or array')
  }

  // Any List<...> anywhere requires the import.
  if (classes.some(c => c.fields.some(f => f.javaType.startsWith('List<')))) {
    usesList = true
  }

  return renderClasses(classes, options.packageName, usesList)
}

/** Best-effort singularization for naming a list's element class (items → Item). */
function singularize(word: string): string {
  if (/ies$/i.test(word)) return word.slice(0, -3) + 'y'
  if (/(ses|xes|zes|ches|shes)$/i.test(word)) return word.slice(0, -2)
  if (/s$/i.test(word) && !/ss$/i.test(word)) return word.slice(0, -1)
  return word
}

/** Render all generated classes into a single .java source string. */
function renderClasses(classes: GeneratedClass[], packageName: string | undefined, usesList: boolean): string {
  const parts: string[] = []

  if (packageName && packageName.trim()) {
    parts.push(`package ${packageName.trim()};\n`)
  }

  const imports: string[] = []
  if (usesList) {
    imports.push('import java.util.List;')
  }
  // @JsonProperty is only needed when at least one field's key differs from its Java name.
  const needsJsonProperty = classes.some(c => c.fields.some(f => f.jsonKey !== f.javaName || !isPlainJavaIdentifier(f.jsonKey)))
  if (needsJsonProperty) {
    imports.push('import com.fasterxml.jackson.annotation.JsonProperty;')
  }
  if (imports.length) {
    parts.push(imports.join('\n') + '\n')
  }

  parts.push(classes.map(c => renderClass(c)).join('\n\n'))

  return parts.join('\n').trimEnd() + '\n'
}

/** Render one class: fields, then getters/setters. */
function renderClass(cls: GeneratedClass): string {
  const lines: string[] = []
  lines.push(`public class ${cls.name} {`)

  // Fields
  for (const f of cls.fields) {
    if (f.jsonKey !== f.javaName || !isPlainJavaIdentifier(f.jsonKey)) {
      lines.push(`    @JsonProperty("${escapeJava(f.jsonKey)}")`)
    }
    lines.push(`    private ${f.javaType} ${f.javaName};`)
  }

  // Getters / setters
  for (const f of cls.fields) {
    const cap = f.javaName.charAt(0).toUpperCase() + f.javaName.slice(1)
    lines.push('')
    lines.push(`    public ${f.javaType} get${cap}() {`)
    lines.push(`        return ${f.javaName};`)
    lines.push('    }')
    lines.push('')
    lines.push(`    public void set${cap}(${f.javaType} ${f.javaName}) {`)
    lines.push(`        this.${f.javaName} = ${f.javaName};`)
    lines.push('    }')
  }

  lines.push('}')
  return lines.join('\n')
}

/** Escape a string for a Java double-quoted literal. */
function escapeJava(s: string): string {
  return s.replace(/\\/g, '\\\\').replace(/"/g, '\\"')
}
