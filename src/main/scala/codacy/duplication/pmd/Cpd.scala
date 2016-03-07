package codacy.duplication.pmd

import java.nio.file.{Path, Paths}

import codacy.dockerApi.api.{DuplicationClone, DuplicationCloneFile, DuplicationConfiguration, Language}
import codacy.dockerApi.traits.IDuplicationImpl
import net.sourceforge.pmd.cpd._

import scala.collection.JavaConversions._
import scala.util.Try

object Cpd extends IDuplicationImpl {
  override def apply(path: Path, config: DuplicationConfiguration): Try[List[DuplicationClone]] = {
    val configuration = getConfiguration(config)
    val cpd = new CPD(configuration)
    cpd.addRecursively(path.toFile)
    cpd.go()
    Try(cpd.getMatches.toList.map(matchToClone(_, path)))
  }

  private def getConfiguration(config: DuplicationConfiguration) = {

    val ignoreAnnotations = true
    val skipLexicalErrors = true

    val (cpdLanguage, defaultMinToken) = config.language match {
      case Language.Ruby => (new RubyLanguage, 20)
      case Language.Java => (new JavaLanguage, 100)
      case Language.Javascript =>
        val cpdJs = new EcmascriptLanguage
        object JavascriptLanguage extends AbstractLanguage(cpdJs.getName, cpdJs.getTerseName, new JavascriptTokenizer(), cpdJs.getExtensions: _*)
        (JavascriptLanguage, 20)
      case Language.Scala =>
        val cpdScala = new ScalaLanguage
        object ScalaL extends AbstractLanguage(cpdScala.getName, cpdScala.getTerseName, ScalaTokenizer, cpdScala.getExtensions: _*)
        (ScalaL, 50)
    }

    val minTokenMatch = config.params.get("minTokenMatch").flatMap(_.asOpt[Int]).getOrElse(defaultMinToken)

    val cfg = new CPDConfiguration()
    cfg.setLanguage(cpdLanguage)
    cfg.setIgnoreAnnotations(ignoreAnnotations)
    cfg.setSkipLexicalErrors(skipLexicalErrors)
    cfg.setMinimumTileSize(minTokenMatch)
    cfg
  }

  private def matchToClone(m: Match, rootDirectory: Path): DuplicationClone = {
    val files = m.getMarkSet.toList.map { mark =>
      val file = rootDirectory.relativize(Paths.get(mark.getFilename))
      DuplicationCloneFile(file.toString, mark.getBeginLine, mark.getEndLine)
    }

    DuplicationClone(m.getSourceCodeSlice, m.getTokenCount, m.getLineCount, files)
  }

}
