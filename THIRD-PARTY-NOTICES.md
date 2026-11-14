# Third-party notices

Minecraftin's original code and generated artwork are covered by the root
`LICENSE`. The dependencies below retain their own licenses. These notices and
all files in `licenses/` accompany `installDist`, `distZip`, and `distTar`.

| Component distributed at runtime | Version | License text | Original source |
| --- | --- | --- | --- |
| LWJGL core, GLFW and OpenGL bindings, including platform natives | 3.4.3 | [BSD 3-Clause](licenses/lwjgl-LICENSE.md) | [LWJGL 3.4.3](https://github.com/LWJGL/lwjgl3/blob/3.4.3/LICENSE.md) |
| libffi (LWJGL core native support) | As bundled by LWJGL 3.4.3 | [MIT](licenses/libffi-LICENSE.txt) | [Bundled notice](https://github.com/LWJGL/lwjgl3/blob/3.4.3/modules/lwjgl/core/libffi_license.txt) |
| liburing (LWJGL core Linux native support) | As bundled by LWJGL 3.4.3 | [MIT](licenses/liburing-LICENSE.txt) | [Bundled notice](https://github.com/LWJGL/lwjgl3/blob/3.4.3/modules/lwjgl/core/liburing_license.txt) |
| GLFW native library | As bundled by LWJGL 3.4.3 | [zlib/libpng](licenses/glfw-LICENSE.txt) | [Bundled notice](https://github.com/LWJGL/lwjgl3/blob/3.4.3/modules/lwjgl/glfw/glfw_license.txt) |
| Khronos OpenGL headers and registry material | As bundled by LWJGL 3.4.3 | [Khronos notice](licenses/khronos-LICENSE.txt) | [Bundled notice](https://github.com/LWJGL/lwjgl3/blob/3.4.3/modules/lwjgl/opengl/khronos_license.txt) |
| JOML | 1.10.9 | [MIT](licenses/joml-LICENSE.txt) | [JOML 1.10.9](https://github.com/JOML-CI/JOML/blob/1.10.9/LICENSE) |

The complete upstream notice files are preserved verbatim. The application
package contains natives for the OS and CPU used to build it; platform-specific
notices are retained in every package. Gradle and JUnit are build/test tools and
are not included in the application runtime distribution. The source checkout
includes Gradle's wrapper scripts and JAR, which retain their upstream notices.
When updating dependencies or adding native modules, review their exact version's
license files and update this inventory before distributing the application.
