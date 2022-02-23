<?php
/**
 * @version		0.1
 * @author		James Frank
 * @package		simple_footnotes_multilingual
 * @copyright		Copyright (C) 2008 James Frank
 * @license		http://www.gnu.org/licenses/gpl-2.0.html GNU/GPL
 * Originally derived from plugin referenced at http://forum.joomla.org/viewtopic.php?f=487&t=247337
 * Multilanguage by Martin Podolak
 */

// Check to ensure this file is included in Joomla!
defined( '_JEXEC' ) or die();

//Import Joomla! plugin related class.
jimport( 'joomla.event.plugin' );

//JPlugin::loadLanguage( 'plg_content_simple_footnotes_multilingual' );


$lang = & JFactory::getLanguage();
$lang->load('plg_content_simple_footnotes_multilingual', JPATH_ADMINISTRATOR);


class plgContentsimple_footnotes_multilingual extends JPlugin
{
	function onBeforeDisplayContent( &$article, &$params, &$limitstart )
	{
		
		//Create instances of Joomla! class
		$document = &JFactory::getDocument();
		$plugin =& JPluginHelper::getPlugin( 'content', 'simple_footnotes_multilingual' );
		$pluginParams = new JParameter( $plugin->params );
		$showBackLinks = $pluginParams->def( 'show_back_links', 1);
	
		if ( JString::strpos( $article->text, '{footnote}' ) > 0 )
		{
		
			if ( strlen($article->fulltext) != 0 AND $article->text == $article->introtext )
			{
				$article->text = preg_replace('/\{footnote\}(.*?)\s*\{\/footnote\}/s',"",$article->text,1);
			}
			else
			{
			
				//Replace footnotes with HTML
				$article->text .= '<div id="footnotes">';
				$article->text .= JText::_('FOOTNOTES');
				
				preg_match_all( "/\{footnote\}(.*?)\s*\{\/footnote\}/s", $article->text, $footnotes );
				$number = 1;
				foreach ($footnotes[1] as $footnote)
				{
					if ( $showBackLinks == 1 )
					{
						if ( array_key_exists('start', $_GET) )
						{
							$startno = $_GET["start"];
							$backLink = " <a href='?start=" . $startno . "#note_" . $number . "'>" . JText::_('BACK') . "</a><br/>";
						}
						else
						{
							$backLink = " <a href='#note_" . $number . "'>" . JText::_('BACK') . "</a><br/>";
						}
						if ( array_key_exists('showall', $_GET) )
						{
							$backLink = " <a href='?showall=1#note_" . $number . "'>" . JText::_('BACK') . "</a><br/>";
						}
					}
					else
					{
						$backLink = "";
					}
					$article->text .= "<a name='note_a_" . $number . "'></a>" . $number . JText::_('COUNT1') . "&nbsp;&nbsp;&nbsp;" . $footnote . $backLink;
					if ( array_key_exists('start', $_GET) )
					{
						$startno = $_GET["start"];
						$anchor = "<a name='note_" . $number . "'></a><a style='position:relative; bottom:5px;' href='?start=" . $startno . "#note_a_" . $number . "'>" . $number . "</a>";
					}
					else
					{
						$anchor = "<a name='note_" . $number . "'></a><a style='position:relative; bottom:5px;' href='#note_a_" . $number . "'>" . $number . "</a>";
					}
					if ( array_key_exists('showall', $_GET) )
					{
						$anchor = "<a name='note_" . $number . "'></a><a style='position:relative; bottom:5px;' href='?showall=1#note_a_" . $number . "'>" . $number . "</a>";
					}				
					$article->text = preg_replace('/\{footnote\}(.*?)\s*\{\/footnote\}/s',$anchor,$article->text,1);
					$number++;
				}
				$article->text .= '</div>' . "\n";
			
			}
		
		}
		
		if ( JString::strpos( $article->text, '{endnote}' ) > 0)
		{
		
			if ( strlen($article->fulltext) != 0 AND $article->text == $article->introtext )
			{
				$article->text = preg_replace('/\{endnote\}(.*?)\s*\{\/endnote\}/s',"",$article->text,1);
			}
			else
			{
		
				//Replace endnotes with HTML
				$article->text .= '<div id="endnotes">';
				$article->text .= JText::_('ENDNOTES');
				
				//Strip out any extra <p> tags around endnotes
				$article->text = preg_replace('|\<p\>\{endnote\}(.*?)\s*\{/endnote\}\s*</p\>|s',"{endnote}$1{/endnote}",$article->text);
				
				preg_match_all( "/\{endnote\}(.*?)\s*\{\/endnote\}/s", $article->text, $endnotes );
				$number = 1;
				foreach ($endnotes[1] as $endnote)
				{
					$article->text .= $number . JText::_('COUNT2') . "&nbsp;&nbsp;&nbsp;" . $endnote . "<br/>";
					$article->text = preg_replace('/\{endnote\}(.*?)\s*\{\/endnote\}/s',"",$article->text,1);
					$number++;
				}
				$article->text .= '</div>' . "\n";
			
			}
		
		}
		
	}
}
