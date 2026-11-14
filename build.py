"""Build the Fuel Siphoning install ZIP against Starsector 0.98.5a-RC5."""

import argparse
import os
from pathlib import Path
import shutil
import subprocess
from tempfile import TemporaryDirectory
from zipfile import ZIP_DEFLATED, ZipFile


ROOT = Path(__file__).resolve().parent
GAME_JARS = ("starfarer.api.jar", "starfarer_obf.jar", "fs.common_obf.jar", "json.jar", "log4j-1.2.9.jar", "lwjgl.jar", "lwjgl_util.jar")
JAR_NAME = "FuelSiphoning.jar"
MOD_FOLDER = "Fuel Siphoning"


def find_game_jars(installation):
    for directory in (installation / "starsector-core", installation / "Contents/Resources/Java", installation):
        jars = [directory / name for name in GAME_JARS]
        if all(jar.is_file() for jar in jars):
            return jars
    raise ValueError(f"Could not find the Starsector jars under {installation}.")


def java_tool(name):
    java_home = os.environ.get("JAVA_HOME")
    if java_home:
        executable = Path(java_home) / "bin" / (name + (".exe" if os.name == "nt" else ""))
        if executable.is_file():
            return str(executable.resolve())
        raise ValueError(f"Could not find {name} in JAVA_HOME.")
    executable = shutil.which(name)
    if executable:
        return executable
    raise ValueError(f"Set JAVA_HOME or add a JDK with {name} to PATH.")


def build(installation, luna_jar):
    if not luna_jar.is_file():
        raise ValueError(f"Could not find LunaLib.jar at {luna_jar}.")

    classpath = os.pathsep.join(str(jar) for jar in [*find_game_jars(installation), luna_jar])
    javac = java_tool("javac")
    jar = java_tool("jar")
    sources = sorted((ROOT / "src").rglob("*.java"))
    dist = ROOT / "dist"
    dist.mkdir(exist_ok=True)

    # Stage the archive beside its final destination so a failed build leaves the previous ZIP usable.
    with TemporaryDirectory(dir=dist) as temp:
        directory = Path(temp)
        classes = directory / "classes"
        classes.mkdir()
        subprocess.run([
            javac, "--release", "17", "-encoding", "UTF-8", "-classpath", classpath,
            "-d", str(classes), *(str(source) for source in sources),
        ], check=True)
        mod_jar = directory / JAR_NAME
        subprocess.run([jar, "--create", "--file", str(mod_jar), "-C", str(classes), "."], check=True)

        archive = directory / "fuel-siphoning.zip"
        with ZipFile(archive, "w", ZIP_DEFLATED) as package:
            for name in ("mod_info.json", "FUEL_SIPHONING_OPTIONS.ini", "sun_fs.version"):
                package.write(ROOT / name, f"{MOD_FOLDER}/{name}")
            for folder in ("data", "sun_fs"):
                for resource in sorted((ROOT / folder).rglob("*")):
                    if resource.is_file():
                        package.write(resource, f"{MOD_FOLDER}/{resource.relative_to(ROOT).as_posix()}")
            package.write(mod_jar, f"{MOD_FOLDER}/jars/{JAR_NAME}")

        output = dist / archive.name
        archive.replace(output)
    return output


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("starsector", type=Path, help="Starsector installation or core jar directory")
    parser.add_argument("lunalib_jar", type=Path, help="Path to LunaLib's jars/LunaLib.jar")
    args = parser.parse_args()
    try:
        print(build(args.starsector.expanduser().resolve(), args.lunalib_jar.expanduser().resolve()))
    except (OSError, ValueError, subprocess.CalledProcessError) as error:
        parser.exit(1, f"{error}\n")


if __name__ == "__main__":
    main()
