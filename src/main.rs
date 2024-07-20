use std::env;

use maplit::hashmap;
use serenity::all::{ChannelId, GatewayIntents};

use ecrit::{
    Ecrit,
    fields::Status,
    fields::Type
};
use fondabots_lib::{
    affichan::Affichan,
    Bot
};

mod ecrit;
mod commands;

pub type DataType = fondabots_lib::DataType<Ecrit>;

#[tokio::main]
async fn main() {
    let args: Vec<String> = env::args().collect();
    if let Some(token) = args.get(1) {
        match Bot::new(
            token.clone(),
            GatewayIntents::GUILD_MESSAGES | GatewayIntents::GUILD_MEMBERS,
            "./critibot.yml",
            commands::command_list(),
            vec![
                Affichan::new(ChannelId::new(843956373103968308), Box::new(|ecrit| {
                    ecrit.status == Status::Ouvert || ecrit.status == Status::OuvertPlus
                })),
                Affichan::new(ChannelId::new(614947463610236939), Box::new(|ecrit| {
                    ecrit.status == Status::OuvertPlus
                })),
                Affichan::new(ChannelId::new(896361827884220467), Box::new(|ecrit| {
                    ecrit.status == Status::Inconnu || ecrit.status == Status::Infraction
                })),
                Affichan::new(ChannelId::new(896362452818747412), Box::new(|ecrit| {
                    ecrit.type_ == Type::Autre
                })),
                Affichan::new(ChannelId::new(554998005850177556), Box::new(|ecrit| {
                    (ecrit.status == Status::Ouvert || ecrit.status == Status::OuvertPlus) &&
                    ecrit.tags.iter().fold(false, |acc, tag| {
                        acc || tag.contains("Concours") || tag.contains("Validation") || tag.contains("Event")
                    })
                }))
            ],
            hashmap! {
                "organichan" => 878917114474410004
            }
        ).await {
            Ok(mut bot) => if let Err(e) = bot.start().await {
                panic!("Erreur lors de l’exécution du bot: {e}");
            }
            Err(e) => panic!("Erreur lors du chargement du bot: {e}")
        }
    }
}